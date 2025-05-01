package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildParams
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.struct.*
import org.gradle.api.Action

/**Stonecutter plugin available in `build.gradle[.kts]`.*/
@StonecutterAPI
public interface StonecutterBuildExtension : StonecutterBuildParams {
    public val tree: ProjectTree
    public val branch: ProjectBranch
    public val node: ProjectNode

    /**
     * Read-only configuration flags container passed from `stonecutter.gradle[.kts]`.
     * Can be used to retrieve default and custom configuration values.
     */
    public val flags: FlagContainer

    /**
     * Structured task container for each stage of file processing.
     * This can be used to programmatically configure task dependencies
     * when the automatic method doesn't work correctly.
     */
    public val tasks: StonecutterBuildTasks

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

    public fun flags(block: Action<FlagContainer>): Unit = block.execute(flags)
}