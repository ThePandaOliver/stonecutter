@file:OptIn(StonecutterInternalAPI::class)

package dev.kikugie.stonecutter.data.tree.builder

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.ProjectReference
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.settings.StonecutterSettingsImpl
import dev.kikugie.stonecutter.util.isIdentifier
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists

private fun ProjectReference.resolve(settings: Settings): ProjectDescriptor = when(this) {
    is ProjectDescriptor -> path.include(settings)
    is CharSequence -> include(settings)
    is Provider<*> -> get().resolve(settings)
    else -> error("Unsupported type ${this::class.qualifiedName}")
}

private fun CharSequence.include(settings: Settings): ProjectDescriptor {
    val trimmed = trimStart(':').toString()
    if (this != ":") settings.include(trimmed)
    return settings.project(":$trimmed")
}

private fun getDefaultBuildscript(ext: StonecutterSettingsImpl, dir: Path, type: String) = when {
    dir.resolve("$type.gradle.kts").exists() -> "$type.gradle.kts"
    dir.resolve("$type.gradle").exists() || ext.isHardMode -> "$type.gradle"
    else -> "$type.gradle.kts"
}

internal abstract class TreeBuilderImpl @Inject constructor(val objects: ObjectFactory) : TreeBuilder() {
    internal var localBuildScriptProvider: ((Identifier, StonecutterProject) -> String)? = null
        private set
    internal val branches: MutableMap<Identifier, BranchBuilderImpl> = mutableMapOf()
    internal val versions: MutableMap<Identifier, StonecutterProject> = mutableMapOf()

    override fun mapBuilds(action: (Identifier, StonecutterProject) -> String) {
        localBuildScriptProvider = action
    }

    override fun branch(name: Identifier, action: BranchBuilder.() -> Unit) {
        require(name.isEmpty() || isIdentifier(name)) { "Invalid branch identifier: '$name'" }
        getOrCreateBranch(name).apply(action)
    }

    override fun inherit() {
        throw UnsupportedOperationException("Root branch has no parent to inherit from")
    }

    override fun versions(versions: List<StonecutterProject>): NodeBuilder =
        getOrCreateBranch("").versions(versions.map { it.project to it.version })

    internal fun getVcsProject() = versions[resolveVcs()]!!

    internal fun getRootNodes(): List<NodeBuilderImpl> = checkNotNull(branches[""]) { "Root branch is not initialised yet" }.nodes

    internal fun registerVariant(version: StonecutterProject): Unit = with(version) {
        val existing = versions.putIfAbsent(project, this)
        require(existing == null || existing == this) { "Project '$project' is registered with a different version '${existing?.version}'" }
    }

    internal fun createWith(ext: StonecutterSettingsImpl, ref: ProjectReference) {
        val project = ref.resolve(ext.settings)
        check(ext.container.putIfAbsent(project.hierarchy, this) == null) { "Project ${project.path} is already registered" }

        val vcs = resolveVcs()
        val controller = controllerType(ext, project)
        with(project.projectDir.resolve(controller.filename).toPath()) {
            project.buildFileName = ext.checkGroovy(fileName.toString())
            if (notExists()) controller.create(this, vcs)
        }

        for (it in branches.values)
            it.createWith(ext, project)
    }

    private fun resolveVcs(): String = vcsVersion.orNull
        ?.also { check(it in versions.keys) { "VCS version '$it' doesn't match any registered subproject" } }
        ?: versions.keys.firstOrNull()
        ?: error("No versions have been registered")

    private fun controllerType(ext: StonecutterSettingsImpl, project: ProjectDescriptor): StonecutterControllerManager {
        val isKotlin = kotlinController.orNull
            ?: ext.kotlinController.orNull
            ?: getDefaultBuildscript(ext, project.projectDir.toPath(), "stonecutter").endsWith("kts")
        return if (isKotlin) StonecutterControllerManager.Kotlin
        else StonecutterControllerManager.Groovy
    }

    private fun getOrCreateBranch(name: String): BranchBuilderImpl =
        branches.getOrPut(name) { objects.newInstance<BranchBuilderImpl>(name, this) }
}

internal abstract class BranchBuilderImpl @Inject constructor(val name: String, val tree: TreeBuilderImpl, val objects: ObjectFactory) :
    BranchBuilder() {
    internal val nodes: MutableList<NodeBuilderImpl> = mutableListOf()

    override fun inherit() {
        nodes += tree.getRootNodes()
    }

    override fun versions(versions: List<StonecutterProject>): NodeBuilder {
        require(versions.isNotEmpty()) { "No versions provided" }
        for (it in versions) tree.registerVariant(it)
        return objects.newInstance<NodeBuilderImpl>(versions, this).also(nodes::add)
    }

    internal fun allProjects(): List<StonecutterProject> = nodes.flatMap { it.versions }.distinct()

    internal fun createWith(ext: StonecutterSettingsImpl, root: ProjectDescriptor) {
        check(nodes.isNotEmpty()) { "Branch '$name' has no registered nodes" }
        val project = if (name.isEmpty()) root else name.resolve(ext.settings)
            .apply { projectDir.toPath().createDirectories() }
        if (project.path != root.path)
            project.buildFileName = ext.checkGroovy(resolveBuild(ext, project))

        for ((data, buildscript) in flattenNodes(ext, project))
            createNode(ext, project, data, buildscript)
    }

    private fun createNode(ext: StonecutterSettingsImpl, parent: ProjectDescriptor, data: StonecutterProject, buildscript: String) {
        val project = "${parent.path}:${data.project}".resolve(ext.settings)
        val directory = parent.projectDir.resolve("versions/${data.project}")
            .apply { toPath().createDirectories() }
        with(project) {
            projectDir = directory
            buildFileName = ext.checkGroovy("../../$buildscript")
        }
    }

    private fun resolveBuild(ext: StonecutterSettingsImpl, project: ProjectDescriptor): String = branchScript.orNull
        ?.also { check(!it.startsWith("stonecutter.gradle")) { "Branch buildscript can't match the controller name" } }
        ?: getDefaultBuildscript(ext, project.projectDir.toPath(), "branch")

    private fun flattenNodes(ext: StonecutterSettingsImpl, project: ProjectDescriptor): Map<StonecutterProject, String> = buildMap {
        val branch = project.projectDir.toPath()
        for (builder in nodes) for (data in builder.versions) {
            val buildscript = builder.buildscript.orNull
                ?: tree.localBuildScriptProvider?.invoke(name, data)
                ?: getDefaultBuildscript(ext, branch.resolve("versions/${data.project}"), "build")
            this[data] = buildscript
        }
    }
}

internal abstract class NodeBuilderImpl @Inject constructor(val versions: List<StonecutterProject>, val branch: BranchBuilderImpl) : NodeBuilder()