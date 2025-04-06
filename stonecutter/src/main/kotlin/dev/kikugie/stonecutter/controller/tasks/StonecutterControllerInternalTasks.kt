package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.process.ControllerExternalUpdateTask
import dev.kikugie.stonecutter.process.ControllerScriptUpdateTask
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

internal class StonecutterControllerInternalTasks {
    val switchTasks: MutableCollection<TaskProvider<out DefaultTask>> = mutableListOf()

    fun switchTaskName(project: Identifier): String = "stonecutterSwitchTo$project"
    fun switchTaskProvider(project: Identifier): TaskProvider<out DefaultTask>? =
        switchTasks.find { it.name == switchTaskName(project) }

    fun registerSwitchTask(project: Identifier, tree: ProjectTree, manager: StonecutterControllerManager): TaskProvider<out DefaultTask> {
        val task = registerMatchingSwitchTask(project, tree, manager)
        switchTasks += task
        return task
    }

    private fun registerMatchingSwitchTask(project: Identifier, tree: ProjectTree, manager: StonecutterControllerManager) =
        if (tree.provider != null) registerExternalSwitchTask(project, tree)
        else registerSelfSwitchTask(project, tree, manager)

    private fun registerSelfSwitchTask(project: Identifier, tree: ProjectTree, manager: StonecutterControllerManager) =
        tree.project.tasks.register<ControllerScriptUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            manager(manager::class.java)
            version(project)
            script.set(tree.project.buildFile)
        }

    private fun registerExternalSwitchTask(project: Identifier, tree: ProjectTree) =
        tree.project.tasks.register<ControllerExternalUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            version(project)
            file.set(tree.provider)
        }
}