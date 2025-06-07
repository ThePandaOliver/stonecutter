package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.tree.model.BranchInfo
import dev.kikugie.stonecutter.data.tree.model.BranchModel
import dev.kikugie.stonecutter.data.tree.model.NodeInfo
import dev.kikugie.stonecutter.data.tree.model.TreeModel
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.process.ControllerExternalUpdateTask
import dev.kikugie.stonecutter.process.ControllerScriptUpdateTask
import dev.kikugie.stonecutter.process.ModelSavingTask
import dev.kikugie.stonecutter.process.StonecutterUpdateTask
import dev.kikugie.stonecutter.util.buildDirectory
import dev.kikugie.stonecutter.util.invoke
import kotlinx.serialization.json.Json
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.register
import java.io.File

@OptIn(StonecutterInternalAPI::class)
internal class StonecutterControllerTasksImpl(val ext: StonecutterControllerImpl) : StonecutterControllerTasks {
    override val switch: MutableTaskProviderMap<Identifier, out StonecutterUpdateTask> = mutableMapOf()
    private val encoder = Json { prettyPrint = true }
    override fun named(name: String, filter: ProjectNode.() -> Boolean): TaskProviderMapProperty<ProjectNode, *> =
        ext.root.objects.mapProperty<ProjectNode, TaskProvider<*>>().value(ext.root.provider {
            ext.tree.nodes
                .filter { filter(it) && name in it.project.tasks.names }
                .associateWith { it.project.tasks.named(name) }
        }).apply { disallowChanges() }

    override fun <T : Task> named(name: String, cls: Class<T>, filter: ProjectNode.() -> Boolean): TaskProviderMapProperty<ProjectNode, T> =
        ext.root.objects.mapProperty<ProjectNode, TaskProvider<T>>().value(ext.root.provider {
            ext.tree.nodes
                .filter { filter(it) && name in it.project.tasks.names }
                .associateWith { it.project.tasks.named(name, cls) }
        }).apply { disallowChanges() }

    override fun order(tasks: TaskProviderMapProperty<ProjectNode, *>, ordering: Comparator<ProjectNode>) = ext.root.afterEvaluate {
        val tasks = tasks().toSortedMap(ordering).values
        val lock = ext.root.buildDirectory.resolve("stonecutter-cache/sc.lock")

        for (it in tasks) it.configure { outputs.file(lock) }
        for ((a, b) in tasks.zipWithNext()) b.configure { mustRunAfter(a) }
    }

    fun registerSelfSwitchTask(project: Identifier, manager: StonecutterControllerManager) =
        ext.root.tasks.register<ControllerScriptUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            manager(manager::class.java)
            version(project)
            script.set(ext.root.buildFile)
        }.also { switch[it.name] = it }

    fun registerExternalSwitchTask(project: Identifier, provider: File) =
        ext.root.tasks.register<ControllerExternalUpdateTask>(switchTaskName(project)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            version(project)
            file.set(provider)
        }.also { switch[it.name] = it }

    fun registerModelGroupingTask() = ext.root.tasks.register("stonecutterSaveModels") {
        group = "stonecutter-impl"
        description = "Internal Stonecutter task. Do not call manually."
    }

    fun registerTreeModelTask() = ext.root.tasks.register<ModelSavingTask>("stonecutterSaveTreeModel") {
        group = "stonecutter-impl"
        description = "Internal Stonecutter task. Do not call manually."

        output.set(ext.root.layout.buildDirectory.file("stonecutter-cache/tree.json"))
        json.set(ext.root.provider {
            val branches = ext.tree.branches.map { BranchInfo(it.id, it.location) }
            val nodes = ext.tree.nodes.map { NodeInfo(it.metadata, it.location) }
            TreeModel(StonecutterPlugin.VERSION, ext.tree.vcs.project, ext.tree.current?.project, branches, nodes, ext.flags)
                .let(encoder::encodeToString)
        })
    }.also {
        ext.root.tasks.named("stonecutterSaveModels") { dependsOn(it) }
    }

    fun registerBranchModelTask(branch: ProjectBranch) = branch.project.tasks.register<ModelSavingTask>("stonecutterSaveBranchModel") {
        group = "stonecutter-impl"
        description = "Internal Stonecutter task. Do not call manually."

        output.set(branch.project.layout.buildDirectory.file("stonecutter-cache/branch.json"))
        json.set(ext.root.provider {
            val nodes = branch.nodes.map { NodeInfo(it.metadata, it.location) }
            BranchModel(branch.id, ext.tree.location, nodes).let(encoder::encodeToString)
        })
    }.also {
        ext.root.tasks.named("stonecutterSaveModels") { dependsOn(it) }
    }
}