package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.controller.manager.ControllerManager
import dev.kikugie.stonecutter.controller.manager.GroovyController
import dev.kikugie.stonecutter.controller.manager.KotlinController
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.settings.StonecutterSettings
import groovy.lang.Closure
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

public abstract class TreeBuilder @Inject constructor(
    private val settings: StonecutterSettings,
) : ProjectProvider {
    init {
        vcsVersion.convention(providers.provider {
            checkNotNull(versions.keys.firstOrNull()?.project) { "No versions registered" }
        })
        kotlinController.convention(settings.kotlinController)
        centralScript.convention(settings.centralScript)
    }

    /**Configures the Version Control Reset project, which is used by the `Reset active project` task.*/
    @StonecutterAPI
    public abstract val vcsVersion: Property<Identifier>
    /**
     * Configures, which format branch controllers uses. Default is `stonecutter.gradle.kts`.
     * Setting it to `false` enables `stonecutter.gradle` with Groovy DSL.
     */
    @StonecutterAPI
    public abstract val kotlinController: Property<Boolean>
    /**
     * Configures the default name for the versioned buildscript.
     * Can be overridden by [TreeBuilder.mapBuilds] or [BranchBuilder.mapBuilds].
     */
    @StonecutterAPI
    public abstract val centralScript: Property<String>

    internal val branches: MutableMap<Identifier, BranchBuilder> = mutableMapOf()
    internal val versions: MutableMap<StonecutterProject, StonecutterProject> = mutableMapOf()
    internal var localBuildscriptProvider: ((Identifier, StonecutterProject) -> String)? = null

    internal val objects: ObjectFactory get() = settings.objects
    internal val providers: ProviderFactory get() = settings.settings.providers
    internal val vcsProject: StonecutterProject
        get() = checkNotNull(findVcsProject()) { "Version '${vcsVersion()}' is not registered" }
    internal val controller: ControllerManager
        get() = if (kotlinController()) KotlinController else GroovyController

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
        require(name.isEmpty() || name.isValid()) { "Invalid branch identifier: '$name'" }
        getOrCreateBranch(name).apply(action)
    }

    /**
     * Provides a naming scheme for versioned buildscripts,
     * which should be determined only by the provided [Identifier] and [StonecutterProject].
     */
    @StonecutterAPI
    public fun mapBuilds(action: (Identifier, StonecutterProject) -> String) {
        localBuildscriptProvider = action
    }

    override fun vers(name: Identifier, version: AnyVersion) {
        getOrCreateBranch("").vers(name, version)
    }

    internal fun identity(vers: StonecutterProject): StonecutterProject =
        versions.getOrPut(vers) { vers }

    internal fun applyData(settings: TreeSettings) {
        settings.vcs?.let(vcsVersion::set)
        settings.kotlinController?.let(kotlinController::set)
        for ((name, projects) in settings.entries) branch(name) {
            for (it in projects) {
                vers(it.entry.project, it.entry.version)
                it.buildscript?.let { b -> nodes[it.entry.project]!!.localScript.set(b) }
            }
        }
    }

    private fun getOrCreateBranch(name: Identifier): BranchBuilder =
        branches.getOrPut(name) { objects.newInstance(name, this) }

    private fun findVcsProject() = versions.values.find { it.project == vcsVersion() }
}

public abstract class BranchBuilder @Inject constructor(
    internal val id: Identifier,
    internal val tree: TreeBuilder,
) : ProjectProvider {
    internal val nodes: MutableMap<Identifier, NodeBuilder> = mutableMapOf()
    internal var localBuildscriptProvider: ((StonecutterProject) -> String)? = null
        get() = field ?: tree.localBuildscriptProvider?.run { { invoke(id, it) } }

    internal val objects: ObjectFactory get() = tree.objects

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
        localBuildscriptProvider = action
    }

    override fun vers(name: Identifier, version: AnyVersion) {
        require(name.isNotBlank() && name.isValid()) { "Invalid project identifier: '$name' in branch '$id'" }
        require(name !in nodes) { "Duplicate project identifier: '$name' in branch '$id'" }

        val identity = tree.identity(StonecutterProject.create(name, version))
        nodes[name] = objects.newInstance(identity, this)
    }
}

public abstract class NodeBuilder @Inject constructor(
    internal val metadata: StonecutterProject,
    internal val branch: BranchBuilder,
) {
    init {
        localScript.convention(branch.tree.centralScript)
    }

    @StonecutterAPI
    public abstract val localScript: Property<String>

    internal val buildscript: String get() = localScript().apply {
        check(isNotBlank()) { "Buildscript must not be blank" }
        check("stonecutter.gradle" !in this) { "Buildscript must not override the controller" }
    }

    private fun localScript(): String = localScript
        .takeIf { it.isPresent }?.get()
        ?: branch.localBuildscriptProvider?.invoke(metadata)
        ?: localScript.get()
}