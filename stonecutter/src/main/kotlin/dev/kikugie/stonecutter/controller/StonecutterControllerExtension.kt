package dev.kikugie.stonecutter.controller

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.controller.flag.MutableFlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasks
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import groovy.lang.Closure
import java.io.File

/**Stonecutter plugin available in `stonecutter.gradle[.kts]`.*/
@StonecutterAPI
public interface StonecutterControllerExtension : VersionOperations<ParsedVersion> {
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

    /**Provides [VersionOperations], which work strictly with [SemanticVersion]s.*/
    public val semantics: VersionOperations<SemanticVersion> get() = SemanticOperations

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
     * Initialises the plugin with the given active version.
     * This function must be called **exactly once**.
     * The provider can be one of:
     * - [File], [Provider<File>][org.gradle.api.provider.Provider], [RegularFileProperty][org.gradle.api.file.RegularFileProperty]:
     *   a text file in UTF-8/ASCII encoding containing **only** the project name.
     *   The file will be overridden when switching versions.
     * - [String]: a literal string passed directly as a function argument.
     *   The build script file will be overridden, replacing the string when switching versions.
     * - `null`: initialises the plugin without attaching the root source.
     *   **This functionality is currently experimental.**
     */
    public infix fun active(provider: Any?)

    public infix fun flags(action: MutableFlagContainer.() -> Unit): Unit = flags.action()
    public fun flags(action: Closure<*>): Unit = flags(action::call)

    public infix fun tasks(action: StonecutterControllerTasks.() -> Unit): Unit = action(tasks)
    public fun tasks(action: Closure<StonecutterControllerTasks>): Unit = tasks(action::call)

    /**
     * Configures [stonecutter parameters][dev.kikugie.stonecutter.build.param.StonecutterBuildConfig] for each subproject in the tree.
     *
     * This configuration is preferred to [subprojects {}][org.gradle.api.Project.subprojects],
     * as it's lazily evaluated when the build plugin is applied to the subproject,
     * instead of resolving it immediately.
     */
    public infix fun parameters(config: StonecutterBuildProperties.() -> Unit)

    /**
     * Configures [stonecutter parameters][dev.kikugie.stonecutter.build.param.StonecutterBuildConfig] for each subproject in the tree.
     *
     * This configuration is preferred to [subprojects {}][org.gradle.api.Project.subprojects],
     * as it's lazily evaluated when the build plugin is applied to the subproject,
     * instead of resolving it immediately.
     */
    public fun parameters(config: Closure<StonecutterBuildProperties>): Unit = parameters(config::call)
}