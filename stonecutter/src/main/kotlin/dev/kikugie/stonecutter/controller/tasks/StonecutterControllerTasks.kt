package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterDevAPI
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.process.StonecutterUpdateTask
import dev.kikugie.stonecutter.util.TaskProviderMap
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Provides structured access to tasks created by [StonecutterController][dev.kikugie.stonecutter.controller.StonecutterControllerExtension].
 */
@StonecutterDevAPI
public interface StonecutterControllerTasks {
    /**
     * All underlying active version switch tasks.
     * Each task depends on all [StonecutterBuildTasks.merge][dev.kikugie.stonecutter.build.task.StonecutterBuildTasks.merge]
     * and updates the active version file if all succeed.
     *
     * `Set active project to ...`, `Reset active project` and `Refresh active project` tasks delegate to these.
     * You can add additional dependencies to these tasks to run when switching versions.
     */
    public val switch: TaskProviderMap<out StonecutterUpdateTask>

    /**Provides a switch task name for the given [StonecutterProject.project][dev.kikugie.stonecutter.data.StonecutterProject.project] name.*/
    public fun switchTaskName(project: Identifier): String = "stonecutterSwitchTo$project"

    /**Provides a switch task provider for the given [StonecutterProject.project][dev.kikugie.stonecutter.data.StonecutterProject.project] name.*/
    public fun switchTaskProvider(project: Identifier): TaskProvider<out StonecutterUpdateTask>? =
        switch[switchTaskName(project)]

    /**
     * Finds tasks in [ProjectTree.nodes][dev.kikugie.stonecutter.data.tree.struct.ProjectTree.nodes] matching the given [name].
     * The list is live and may be empty if realised before subprojects are evaluated.
     * It can be used as-is in [Task.dependsOn], where it's final value will be used.
     */
    public fun named(name: String): ListProperty<TaskProvider<*>> = named(name) { true }
    /**
     * Finds tasks in [ProjectTree.nodes][dev.kikugie.stonecutter.data.tree.struct.ProjectTree.nodes] matching the given [name] and [filter].
     * The list is live and may be empty if realised before subprojects are evaluated.
     * It can be used as-is in [Task.dependsOn], where it's final value will be used.
     */
    public fun named(name: String, filter: ProjectNode.() -> Boolean): ListProperty<TaskProvider<*>>

    /**
     * Finds tasks in [ProjectTree.nodes][dev.kikugie.stonecutter.data.tree.struct.ProjectTree.nodes] matching the given [name] and [class][cls].
     * The list is live and may be empty if realised before subprojects are evaluated.
     * It can be used as-is in [Task.dependsOn], where it's final value will be used.
     */
    public fun <T : Task> named(name: String, cls: Class<T>): ListProperty<TaskProvider<T>> = named(name, cls) { true }

    /**
     * Finds tasks in [ProjectTree.nodes][dev.kikugie.stonecutter.data.tree.struct.ProjectTree.nodes] matching the given [name], [class][cls] and [filter].
     * The list is live and may be empty if realised before subprojects are evaluated.
     * It can be used as-is in [Task.dependsOn], where it's final value will be used.
     */
    public fun <T : Task> named(name: String, cls: Class<T>, filter: ProjectNode.() -> Boolean): ListProperty<TaskProvider<T>>
}