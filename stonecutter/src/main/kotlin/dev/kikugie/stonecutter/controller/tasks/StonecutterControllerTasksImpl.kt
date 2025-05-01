package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import dev.kikugie.stonecutter.process.ControllerExternalUpdateTask
import dev.kikugie.stonecutter.process.ControllerScriptUpdateTask
import dev.kikugie.stonecutter.process.StonecutterUpdateTask
import dev.kikugie.stonecutter.util.MutableTaskProviderMap
import dev.kikugie.stonecutter.util.invoke
import org.gradle.kotlin.dsl.register
import java.io.File

@OptIn(StonecutterInternalAPI::class)
internal class StonecutterControllerTasksImpl : StonecutterControllerTasks {
    override val switch: MutableTaskProviderMap<out StonecutterUpdateTask> = mutableMapOf()

    fun registerSelfSwitchTask(project: Identifier, tree: ProjectTree, manager: StonecutterControllerManager) =
        tree.project.tasks.register<ControllerScriptUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            manager(manager::class.java)
            version(project)
            script.set(tree.project.buildFile)
        }.also { switch[it.name] = it }

    fun registerExternalSwitchTask(project: Identifier, tree: ProjectTree, provider: File) =
        tree.project.tasks.register<ControllerExternalUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            version(project)
            file.set(provider)
        }.also { switch[it.name] = it }
}