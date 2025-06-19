package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.param.DeprecatedBuildConfig
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import groovy.lang.Closure

/**Stonecutter plugin available in `build.gradle[.kts]`.*/
@StonecutterAPI
public interface StonecutterBuildExtension : DeprecatedBuildConfig {
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

    public infix fun flags(action: FlagContainer.() -> Unit): Unit = flags.action()
    public fun flags(action: Closure<*>): Unit = flags(action::call)

    public infix fun tasks(action: StonecutterBuildTasks.() -> Unit): Unit = tasks.action()
    public fun tasks(action: Closure<*>): Unit = tasks(action::call)
}