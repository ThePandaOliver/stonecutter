package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterDevAPI
import dev.kikugie.stonecutter.process.StonecutterUpdateTask
import dev.kikugie.stonecutter.util.TaskProviderMap
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
    @StonecutterDevAPI public val switch: TaskProviderMap<out StonecutterUpdateTask>

    /**Provides a switch task name for the given [StonecutterProject.project][dev.kikugie.stonecutter.data.StonecutterProject.project] name.*/
    @StonecutterDevAPI
    public fun switchTaskName(project: Identifier): String = "stonecutterSwitchTo$project"

    /**Provides a switch task provider for the given [StonecutterProject.project][dev.kikugie.stonecutter.data.StonecutterProject.project] name.*/
    @StonecutterDevAPI
    public fun switchTaskProvider(project: Identifier): TaskProvider<out StonecutterUpdateTask>? =
        switch[switchTaskName(project)]
}