package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.dsl.*
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import groovy.lang.Closure
import org.gradle.api.tasks.util.PatternFilterable

@StonecutterAPI
public interface StonecutterBuildConfig : VersionOperations<ParsedVersion> {
    public val node: ProjectNode
    public val branch: ProjectBranch get() = node.branch
    public val tree: ProjectTree get() = branch.tree

    /**
     * Metadata of the active subproject, which has root `src/` sources assigned to it.
     */
    public val active: StonecutterProject?
        get() = tree.current

    /**
     * Metadata of the subproject assigned to this instance of the versioned buildscript.
     * Can be active, which can be checked with `stonecutter.current.isActive`
     * or `stonecutter.current == stonecutter.active`.
     */
    public val current: StonecutterProject
        get() = node.metadata

    /**
     * All subproject metadata entries in this branch (or project when a single branch is used).
     * Entries maintain instance identity:
     * ```kotlin
     * assert(stonecutter.versions.first { it.isActive } === stonecutter.active)
     * ```
     */
    public val versions: Collection<StonecutterProject>
        get() = branch.versions

    /**
     * Configures constants available in the comment processor.
     * Constants evaluate directly as [Boolean], which is often used
     * for mod loader checks.
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/params#condition-constants">Stonecutter wiki</a>
     */
    public val constants: ConstantContainer

    /**
     * Configures dependencies available in the comment processor.
     * Dependencies allow decoupling the compatibility code
     * from the defined standard versions.
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/params#condition-dependencies">Stonecutter wiki</a>
     */
    public val dependencies: DependencyContainer

    /**
     * Configures swaps available in the comment processor.
     * Swaps allow replacing common code snippets to avoid boilerplate conditions.
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/params#string-swaps">Stonecutter wiki</a>
     */
    public val swaps: SwapContainer

    /**
     * Configures replacements applied to the processed files.
     * Unlike [swaps], replacements operate on all files, replacing matched patterns.
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/params#replacements">Stonecutter wiki</a>
     */
    public val replacements: ReplacementContainer
    public val filters: PatternFilterable

    /**Provides [VersionOperations], which work strictly with [SemanticVersion]s.*/
    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations

    /**Configures [constants] with the provided [action].*/
    public fun constants(action: ConstantContainer.() -> Unit): Unit = constants.action()

    /**Configures [constants] with the provided [action].*/
    public fun constants(action: Closure<*>): Unit = constants(action::call)

    /**Configures [dependencies] with the provided [action].*/
    public fun dependencies(action: DependencyContainer.() -> Unit): Unit = dependencies.action()

    /**Configures [dependencies] with the provided [action].*/
    public fun dependencies(action: Closure<*>): Unit = dependencies(action::call)

    /**Configures [swaps] with the provided [action].*/
    public fun swaps(action: SwapContainer.() -> Unit): Unit = swaps.action()

    /**Configures [swaps] with the provided [action].*/
    public fun swaps(action: Closure<*>): Unit = swaps(action::call)

    /**Configures [replacements] with the provided [action].*/
    public fun replacements(action: ReplacementContainer.() -> Unit): Unit = replacements.action()

    /**Configures [replacements] with the provided [action].*/
    public fun replacements(action: Closure<*>): Unit = replacements(action::call)

    /**Configures [filters] with the provided [action].*/
    public fun filters(action: PatternFilterable.() -> Unit): Unit = filters.action()

    /**Configures [filters] with the provided [action].*/
    public fun filters(action: Closure<*>): Unit = filters(action::call)
}

