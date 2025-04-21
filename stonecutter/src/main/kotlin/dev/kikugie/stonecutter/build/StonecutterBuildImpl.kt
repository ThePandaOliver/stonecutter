package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.build.dsl.ConstantContainer
import dev.kikugie.stonecutter.build.dsl.DependencyContainer
import dev.kikugie.stonecutter.build.dsl.FilterContainer
import dev.kikugie.stonecutter.build.dsl.ReplacementContainer
import dev.kikugie.stonecutter.build.dsl.SwapContainer
import dev.kikugie.stonecutter.build.param.FilterContainerImpl
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

internal open class StonecutterBuildImpl(val project: Project) : StonecutterBuildExtension {
    override val tasks: StonecutterBuildTasksImpl = StonecutterBuildTasksImpl(this)
    override val tree: ProjectTree by lazy { project.findProjectTree() }
    override val branch: ProjectBranch get() = tree.getChecked(parent.hierarchy) { "Branch for '$it' not found in ${tree.hierarchy}: ${keysToString()}" }
    override val node: ProjectNode get() = branch.getChecked(project.hierarchy) { "Node for '$it' not found in ${branch.hierarchy}: ${keysToString()}" }
    override val flags: FlagContainer get() = controller.flags

    override val constants: ConstantContainer get() = data.constants
    override val dependencies: DependencyContainer get() = data.dependencies
    override val swaps: SwapContainer get() = data.swaps
    override val replacements: ReplacementContainer get() = data.replacements
    override val filters: FilterContainer get() = data.filters

    internal val parent: Project get() = project.parent!!
    internal val controller: StonecutterControllerImpl get() = tree.getControllerImpl()
    internal val data: StonecutterBuildData get() = controller.getOrCreateParameters(project.hierarchy)

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

        with(filters as FilterContainerImpl) {
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

        val generateTask = tasks.registerGenerateTask(src) {
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
            dependsOn(generateTask)
        }
    }
}