package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import dev.kikugie.stonecutter.process.ControllerExternalUpdateTask
import dev.kikugie.stonecutter.process.ControllerScriptUpdateTask
import dev.kikugie.stonecutter.process.StonecutterUpdateTask
import dev.kikugie.stonecutter.util.MutableTaskProviderMap
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.register
import java.io.File

@OptIn(StonecutterInternalAPI::class)
internal class StonecutterControllerTasksImpl(val ext: StonecutterControllerImpl) : StonecutterControllerTasks {
    override val switch: MutableTaskProviderMap<out StonecutterUpdateTask> = mutableMapOf()
    override fun named(name: String, filter: ProjectNode.() -> Boolean): ListProperty<TaskProvider<*>> =
        ext.root.objects.listProperty<TaskProvider<*>>().value(ext.root.provider {
            ext.tree.nodes.filter(filter).mapNotNull { if (name in it.project.tasks.names) it.project.tasks.named(name) else null }
        }).apply { disallowChanges() }

    override fun <T : Task> named(name: String, cls: Class<T>, filter: ProjectNode.() -> Boolean): ListProperty<TaskProvider<T>> =
        ext.root.objects.listProperty<TaskProvider<T>>().value(ext.root.provider {
            ext.tree.nodes.filter(filter).mapNotNull { if (name in it.project.tasks.names) it.project.tasks.named(name, cls) else null }
        }).apply { disallowChanges() }

    fun registerSelfSwitchTask(project: Identifier, tree: ProjectTree, manager: StonecutterControllerManager) =
        tree.project.tasks.register<ControllerScriptUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            manager(manager::class.java)
            version(project)
            script.set(tree.project.buildFile)
            dependsOn()
        }.also { switch[it.name] = it }

    fun registerExternalSwitchTask(project: Identifier, tree: ProjectTree, provider: File) =
        tree.project.tasks.register<ControllerExternalUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            version(project)
            file.set(provider)
        }.also { switch[it.name] = it }
}