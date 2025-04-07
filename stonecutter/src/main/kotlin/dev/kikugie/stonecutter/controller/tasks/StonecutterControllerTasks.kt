package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.process.StonecutterUpdateTask
import dev.kikugie.stonecutter.util.TaskProviderMap
import org.gradle.api.tasks.TaskProvider

public interface StonecutterControllerTasks {
    public val switch: TaskProviderMap<out StonecutterUpdateTask>

    public fun switchTaskName(project: Identifier): String = "stonecutterSwitchTo$project"
    public fun switchTaskProvider(project: Identifier): TaskProvider<out StonecutterUpdateTask>? =
        switch[switchTaskName(project)]
}