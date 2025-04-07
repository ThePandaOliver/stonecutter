package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.build.param.StonecutterBuildParams
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.getChecked
import dev.kikugie.stonecutter.keysToString
import dev.kikugie.stonecutter.process.FileProcessingData
import dev.kikugie.stonecutter.util.*
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet

internal open class StonecutterBuildImpl @JvmOverloads constructor(
    val project: Project,
    final override val tree: ProjectTree = project.findProjectTree(),
    val controller: StonecutterControllerImpl = tree.getControllerImpl(),
    val data: StonecutterBuildData = controller.getOrCreateParameters(project.hierarchy),
) : StonecutterBuildExtension, StonecutterBuildParams by data {
    final override val branch: ProjectBranch = tree.getChecked(project.parent!!.hierarchy) {
        "Branch for '$it' not found in ${tree.hierarchy}: ${keysToString()}"
    }
    final override val node: ProjectNode = branch.getChecked(project.hierarchy) {
        "Node for '$it' not found in ${branch.hierarchy}: ${keysToString()}"
    }
    override val tasks: StonecutterBuildTasksImpl = StonecutterBuildTasksImpl(this)
    override val flags: FlagContainer get() = controller.flags
    internal val parent: Project get() = project.parent!!

    init {
        configureProject()
    }

    private fun configureProject() = with(project) {
        plugins.apply("java")
        sourceSets.all {
            createProcessingTasks(this)
            this@StonecutterBuildImpl.tasks.configureSource(this)
        }
        configureTaskDependencies()
    }

    private fun configureTaskDependencies() = project.afterEvaluate {
        if (flags[StonecutterFlag.APPEND_SOURCES_AFTER_EVAL]) sourceSets.forEach(this@StonecutterBuildImpl.tasks::configureSource)
        if (flags[StonecutterFlag.GENERATE_SOURCES_ON_SYNC] && isIdeaSync) this@StonecutterBuildImpl.tasks.generate.keys.map { "$path:$it" }
            .let { gradle.requestTasks(it, path, projectDir) }

        controller.tasks.switchTaskProvider(current.project)?.configure {
            dependsOn(this@StonecutterBuildImpl.tasks.merge)
        }

        with(filter) {
            for (it in this@StonecutterBuildImpl.tasks.prepare.values) it.configure {
                sources.from(files(sources.files).asFileTree.filter { filter(it.toPath()) })
            }
        }
    }

    private fun createProcessingTasks(src: SourceSet) {
        val prepareTask = tasks.registerPrepareTask(src) {
            sources.from(parent.projectDirectory.resolve("src/${src.name}"))
            root.set(parent.projectDirectory.resolve("src/${src.name}"))
            caches.set(tasks.processedCacheDir.resolve(src.name))
            project.provider { data.asProcessingData(flags[StonecutterFlag.IMPLICIT_RECEIVER], current.version) }
                .let<Provider<FileProcessingData>, Unit>(parameters::set)
        }

        tasks.registerGenerateTask(src) {
            root.set(parent.projectDirectory.resolve("src/${src.name}"))
            source.set(project.projectDirectory.resolve("src/${src.name}"))
            cache.set(tasks.processedCacheDir.resolve(src.name))

            sources.from(parent.projectDirectory.resolve("src/${src.name}"))
            excludes.from(project.layout.projectDirectory.dir("src/${src.name}"))
            processed.set(tasks.processedCacheDir.resolve(src.name))
            generated.set(tasks.generatedSourcesDir.resolve(src.name))
            dependsOn(prepareTask)
        }

        tasks.registerMergeTask(src) {
            from(tasks.processedCacheDir.resolve(src.name))
            into(parent.projectDirectory.resolve("src/${src.name}"))
            dependsOn(prepareTask)
        }
    }
}