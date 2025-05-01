package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterUtility
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.controller.flag.MutableFlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasks
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import groovy.lang.Closure
import java.io.File

/**Stonecutter plugin available in `stonecutter.gradle[.kts]`.*/
@StonecutterAPI
public interface StonecutterControllerExtension : StonecutterUtility {
    public val tree: ProjectTree

    /**Active version assigned by [active] function.*/
    public val current: StonecutterProject? get() = tree.current

    /**VCS project assigned during tree construction.*/
    public val vcsVersion: StonecutterProject get() = tree.vcs

    /**
     * All unique versions in the tree. Unlike versions in branches,
     * these may contain duplicate [StonecutterProject.project] entries.
     */
    public val versions: Collection<StonecutterProject> get() = tree.versions

    /**
     * Structured task container for version switches,
     * which can be used to programmatically add hooks to
     * corresponding tasks.
     */
    public val tasks: StonecutterControllerTasks

    /**
     * Mutable container for Stonecutter configuration flags.
     * Its values can be accessed in [StonecutterBuild][dev.kikugie.stonecutter.build.StonecutterBuildExtension].
     * @see StonecutterFlag.Companion
     */
    public val flags: MutableFlagContainer

    /**Assigns provided flag the given [value].*/
    public infix fun <T : Any> StonecutterFlag<T>.assign(value: T): Unit =
        flags.set(this, value)

    /**
     * Assigns the given [StonecutterProject.project] name as the tree's active entry.
     * When switching versions, `stonecutter.gradle[.kts]` will be updated.
     * - **The function call should be given a literal string, or the active version won't be updated.**
     * - **This function must be called exactly once, otherwise an exception will be thrown.**
     */
    public infix fun active(name: Identifier)

    /**
     * Assigns the given [file] content as the tree's active entry.
     * When switching versions, the [file] will be updated.
     * - **The provided file must contain the name of the active project on the first line in UTF-8 encoding.**
     * - **This function must be called exactly once, otherwise an exception will be thrown.**
     */
    public infix fun active(file: File)

    /**
     * Configures [stonecutter parameters][dev.kikugie.stonecutter.build.param.StonecutterBuildParams] for each subproject in the tree.
     *
     * This configuration is preferred to [subprojects {}][org.gradle.api.Project.subprojects],
     * as it's lazily evaluated when the build plugin is applied to the subproject,
     * instead of resolving it immediately.
     */
    public infix fun parameters(config: StonecutterDelegatedBuildParams.() -> Unit)

    /**
     * Configures [stonecutter parameters][dev.kikugie.stonecutter.build.param.StonecutterBuildParams] for each subproject in the tree.
     *
     * This configuration is preferred to [subprojects {}][org.gradle.api.Project.subprojects],
     * as it's lazily evaluated when the build plugin is applied to the subproject,
     * instead of resolving it immediately.
     */
    public fun parameters(config: Closure<StonecutterDelegatedBuildParams>): Unit =
        parameters(config::call)
}