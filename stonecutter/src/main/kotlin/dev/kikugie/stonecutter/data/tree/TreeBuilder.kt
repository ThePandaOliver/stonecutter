package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.controller.manager.GroovyController
import dev.kikugie.stonecutter.controller.manager.KotlinController
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.settings.ProjectProvider
import dev.kikugie.stonecutter.settings.SettingsAbstraction
import dev.kikugie.stonecutter.settings.StonecutterSettings
import org.gradle.api.Action
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Represents a project tree structure in the `settings.gradle[.kts]` file.
 * This tree only supports three layers of depth: `root -> branch -> node`.
 */
public class TreeBuilder internal constructor() : ProjectProvider {
    internal constructor(settings: SettingsAbstraction) : this() {
        kotlinController = settings.kotlinController
        centralScript = settings.centralScript
    }

    /**
     * Enables Kotlin buildscripts for the controller.
     * - `stonecutter.gradle` -> `stonecutter.gradle.kts`
     */
    @StonecutterAPI public var kotlinController: Boolean = false

    /**Buildscript used by all subprojects. Defaults to `build.gradle`.*/
    @StonecutterAPI public var centralScript: String = "build.gradle"
        set(value) {
            require(!value.startsWith("stonecutter.gradle")) { "Build script must not override the controller" }
            field = value
        }

    internal val versions: MutableMap<StonecutterProject, StonecutterProject> = mutableMapOf()
    internal val nodes: MutableMap<Identifier, MutableMap<Identifier, StonecutterProject>> = mutableMapOf()
    internal val branches: MutableMap<Identifier, BranchBuilder> = mutableMapOf()

    /**Version used by the `Reset active project` task. Defaults to the first registered version.*/
    @StonecutterAPI public var vcsVersion: Identifier by VcsDelegate()
    internal val vcsProject: StonecutterProject
        get() {
            check(versions.isNotEmpty()) { "No versions registered" }
            val vcs = versions.values.find { it.project == vcsVersion }
            checkNotNull(vcs) { "VCS version '$vcsVersion' not registered" }
            return vcs
        }
    internal val controller get() = if (kotlinController) KotlinController else GroovyController

    internal fun add(branch: Identifier, project: StonecutterProject) {
        val identity = versions.getOrPut(project) { project }
        val previous = nodes.getOrPut(branch) { mutableMapOf() }.put(identity.project, identity)
        require(previous == null) { "Duplicate project path for '$previous' and '$identity' in branch '$branch'" }
    }

    override fun vers(name: Identifier, version: AnyVersion): Unit =
        add("", StonecutterProject.create(name, version))

    /**Creates an inherited branch, which copies all the versions specified in this block.*/
    @StonecutterAPI public fun branch(name: Identifier): Unit = branch(name) { inherit() }

    /**Creates a new branch in this tree with the provided configuration.*/
    @StonecutterAPI public fun branch(name: Identifier, action: Action<BranchBuilder>) {
        require(name.isNotBlank()) { "Branch name cannot be blank" }
        require(name.isValid()) { "Invalid branch name: '$name'" }
        branches.getOrPut(name) { BranchBuilder(this, name) }.let(action::execute)
    }

    private inner class VcsDelegate() : ReadWriteProperty<Any?, Identifier> {
        var value: Identifier? = null
        override fun getValue(thisRef: Any?, property: KProperty<*>): Identifier =
            value ?: checkNotNull(versions.values.firstOrNull()?.project) { "No versions registered" }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Identifier) {
            require(value.isNotBlank()) { "VCS version cannot be blank" }
            require(value.isValid()) { "Invalid VCS version: '$value'" }
            this.value = value
        }
    }
}

/**
 * Proxy class for adding nodes to the given branch in the tree.
 *
 * @param id Subproject's name for this branch
 */
public class BranchBuilder internal constructor(private val tree: TreeBuilder, private val id: Identifier) : ProjectProvider {
    private var _buildscript: String? = null

    /**
     * Buildscript filename overrides for this branch.
     * Defaults to [StonecutterSettings.centralScript].
     */
    public var buildscript: String
        get() = _buildscript ?: tree.centralScript
        set(value) {
            require(!value.startsWith("stonecutter.gradle")) { "Build script must not override the controller" }
            _buildscript = value
        }

    override fun vers(name: Identifier, version: AnyVersion): Unit =
        tree.add(id, StonecutterProject.create(name, version))

    /**
     * Copies nodes registered in [TreeBuilder] to this branch
     */
    @StonecutterAPI public fun inherit(): Unit = tree.nodes.getChecked("") { "Main branch has no registered nodes" }
        .forEach { tree.add(id, it.value) }
}