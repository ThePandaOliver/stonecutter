package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterDevAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildParams
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree

/**
 * Stonecutter plugin available in `build.gradle[.kts]`.
 */
@StonecutterAPI
public interface StonecutterBuildExtension : StonecutterBuildParams {
    @StonecutterAPI public val tree: ProjectTree
    @StonecutterAPI public val branch: ProjectBranch
    @StonecutterAPI public val node: ProjectNode
    /**
     * Read-only configuration flags container passed from `stonecutter.gradle[.kts]`.
     * Can be used to retrieve default and custom configuration values.
     */
    @StonecutterDevAPI public val flags: FlagContainer

    /**
     * Structured task container for each stage of file processing.
     * This can be used to programmatically configure task dependencies
     * when the automatic method doesn't work correctly.
     */
    @StonecutterDevAPI public val tasks: StonecutterBuildTasks

    /**
     * Metadata of the active subproject, which has root `src/` sources assigned to it.
     */
    @StonecutterAPI public val active: StonecutterProject
        get() = tree.current

    /**
     * Metadata of the subproject assigned to this instance of the versioned buildscript.
     * Can be active, which can be checked with `stonecutter.current.isActive`
     * or `stonecutter.current == stonecutter.active`.
     */
    @StonecutterAPI public val current: StonecutterProject
        get() = node.metadata

    /**
     * All subproject metadata entries in this branch (or project when a single branch is used).
     * Entries maintain instance identity:
     * ```kotlin
     * assert(stonecutter.versions.first { it.isActive } === stonecutter.active)
     * ```
     */
    @StonecutterAPI public val versions: Collection<StonecutterProject>
        get() = branch.versions
}