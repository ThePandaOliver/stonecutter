package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.SCDocumentation
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.settings.StonecutterSettingsImpl
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.isIdentifier
import groovy.lang.Closure
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@Suppress("LeakingThis")
@SCDocumentation("settings.create")
public abstract class TreeBuilder @Inject internal constructor(
    internal val settings: StonecutterSettingsImpl,
) : ProjectProvider() {
    init {
        vcsVersion.convention(settings.providers.provider {
            checkNotNull(versions.keys.firstOrNull()?.project) { "No versions registered" }
        })
    }

    /**Configures the Version Control Reset project, which is used by the `Reset active project` task.*/
    @SCDocumentation("settings.vcs")
    @StonecutterAPI
    public abstract val vcsVersion: Property<Identifier>

    /**
     * Configures, which format branch controllers uses. Default is `stonecutter.gradle.kts`.
     * Setting it to `false` enables `stonecutter.gradle` with Groovy DSL.
     */
    @SCDocumentation("settings.create")
    @StonecutterAPI
    public abstract val kotlinController: Property<Boolean>

    /**
     * Configures the default name for the versioned buildscript.
     * Can be overridden, in order of priority, by:
     * 1. [NodeProvider.buildscript]
     */
    @SCDocumentation("settings.create")
    @StonecutterAPI
    public abstract val centralScript: Property<String>

    internal val branches: MutableMap<Identifier, BranchBuilder> = mutableMapOf()
    internal val versions: MutableMap<StonecutterProject, StonecutterProject> = mutableMapOf()
    private var localBuildScriptProvider: ((Identifier, StonecutterProject) -> String)? = null

    internal val vcsProject: StonecutterProject
        get() = checkNotNull(findVcsProject()) { "Version '${vcsVersion()}' is not registered" }

    /**
     * Creates retrieves an inherited branch with the given [name], which copies all versions specified in this block.
     * @throws IllegalArgumentException If the branch name is not a valid identifier
     * @throws IllegalStateException If the main branch is not initialized
     */
    @StonecutterAPI
    public fun branch(name: Identifier): Unit =
        branch(name) { inherit() }

    /**
     * Creates a retrieves a branch with the provided Groovy configuration.
     * @throws IllegalArgumentException If the branch name is not a valid identifier
     */
    @StonecutterAPI
    public fun branch(name: Identifier, closure: Closure<BranchBuilder>): Unit =
        branch(name) { closure.call(this) }

    /**
     * Creates or retrieves a branch with the provided Kotlin configuration.
     * @throws IllegalArgumentException If the branch name is not a valid identifier
     */
    @StonecutterAPI
    public fun branch(name: Identifier, action: BranchBuilder.() -> Unit) {
        require(isIdentifier(name)) { "Invalid branch identifier: '$name'" }
        getOrCreateBranch(name).apply(action)
    }

    /**
     * Provides a naming scheme for versioned buildscripts,
     * which should be determined only by the provided [Identifier] and [StonecutterProject].
     */
    @StonecutterAPI
    public fun mapBuilds(action: (Identifier, StonecutterProject) -> String) {
        localBuildScriptProvider = action
    }

    override fun versions(versions: Iterable<StonecutterProject>): NodeProvider =
        getOrCreateBranch("").versions(versions)

    internal fun identity(vers: StonecutterProject): StonecutterProject =
        versions.getOrPut(vers) { vers }

    internal fun applyData(settings: TreeSettings) {
        settings.vcs?.let(vcsVersion::set)
        settings.kotlinController?.let(kotlinController::set)
        for ((name, projects) in settings.entries) branch(name) {
            for (it in projects) {
                vers(it.entry.project, it.entry.version)
                it.buildscript?.let { b -> nodes[it.entry.project]!!.buildscript = b }
            }
        }
    }

    internal fun buildScriptFor(branch: Identifier, project: StonecutterProject): String =
        localBuildScriptProvider?.invoke(branch, project)
            ?: centralScript.orNull
            ?: settings.centralScript.orNull
            ?: settings.getDefaultBuildScript(branch, "build")

    internal fun controllerTypeFor(branch: Identifier = ""): StonecutterControllerManager =
        if (isKotlinController(branch)) StonecutterControllerManager.Kotlin else StonecutterControllerManager.Groovy

    private fun isKotlinController(branch: Identifier = ""): Boolean = kotlinController.orNull
        ?: settings.kotlinController.orNull
        ?: settings.getDefaultBuildScript(branch, "stonecutter").endsWith("kts")

    private fun getOrCreateBranch(name: Identifier): BranchBuilder =
        branches.getOrPut(name) { settings.objects.newInstance(name, this) }

    private fun findVcsProject() = versions.values.find { it.project == vcsVersion() }
}

public abstract class BranchBuilder @Inject constructor(
    internal val id: Identifier,
    internal val tree: TreeBuilder,
) : ProjectProvider() {
    internal val nodes: MutableMap<Identifier, NodeBuilder> = mutableMapOf()
    private var localBuildScriptProvider: ((StonecutterProject) -> String)? = null

    /**
     * Adds versions currently present in the main branch.
     * @throws IllegalStateException If the main branch is not initialized
     */
    @StonecutterAPI
    public fun inherit(): Unit = checkNotNull(tree.branches[""]) { "Main branch has no registered nodes" }
        .nodes.values.map { it.metadata }.forEach { vers(it.project, it.version) }

    /**
     * Provides a naming scheme for versioned buildscripts,
     * which should be determined only by the provided [StonecutterProject].
     */
    @StonecutterAPI
    public fun mapBuilds(action: (StonecutterProject) -> String) {
        localBuildScriptProvider = action
    }

    override fun versions(versions: Iterable<StonecutterProject>): NodeProvider = versions.map {
        require(it.project !in nodes) { "Duplicate project identifier: '${it.project}' in branch '$id'" }
        NodeBuilder(tree.identity(it), this).apply { nodes[it.project] = this }
    }.let(::NodeProvider)

    internal fun buildScriptFor(project: StonecutterProject) =
        localBuildScriptProvider?.invoke(project)
            ?: tree.buildScriptFor(id, project)
}

internal class NodeBuilder(
    internal val metadata: StonecutterProject,
    internal val branch: BranchBuilder,
) {
    var buildscript: String
        get() = (buildScriptOverride ?: branch.buildScriptFor(metadata)).apply {
            check(isNotBlank()) { "Buildscript must not be blank" }
            check("stonecutter.gradle" !in this) { "Buildscript must not override the controller" }
        }
        set(value) {
            buildScriptOverride = value
        }
    private var buildScriptOverride: String? = null
}

/**Provides a wrapper for one or more [NodeBuilder]s to link custom buildscript files.*/
public class NodeProvider internal constructor(private val builders: Iterable<NodeBuilder>) {
    public var buildscript: String
        @Deprecated("Write-only property", level = DeprecationLevel.HIDDEN) get() = error("")
        set(value) = buildscript(value)

    public fun buildscript(name: String): Unit = builders.forEach { it.buildscript = name }
}