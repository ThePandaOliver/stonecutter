package dev.kikugie.stonecutter.build

import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stonecutter.data.dsl.ConstantContainer
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.dsl.DependencyContainer
import dev.kikugie.stonecutter.data.dsl.FilterContainer
import dev.kikugie.stonecutter.data.dsl.ReplacementContainer
import dev.kikugie.stonecutter.data.dsl.SwapContainer
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.FilterContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import dev.kikugie.stonecutter.getChecked
import dev.kikugie.stonecutter.keysToString
import dev.kikugie.stonecutter.util.*
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

internal open class StonecutterBuildImpl(val project: Project) : StonecutterBuildExtension,
VersionOperations<ParsedVersion> by LenientOperations {
    override val tasks: StonecutterBuildTasksImpl = StonecutterBuildTasksImpl(this)
    override val tree: ProjectTree by lazy { project.gradle.getContainer<ProjectTreeContainer>().projects.getChecked(project.hierarchy) {
        "Tree for $it not found: ${keysToString()}"
    } }
    override val branch: ProjectBranch by lazy { tree.getChecked(parent.hierarchy) { "Branch for '$it' not found in ${tree.hierarchy}: ${keysToString()}" } }
    override val node: ProjectNode by lazy { branch.getChecked(project.hierarchy) { "Node for '$it' not found in ${branch.hierarchy}: ${keysToString()}" } }
    override val flags: FlagContainer get() = controller.flags

    override val constants: ConstantContainer get() = data.constants
    override val dependencies: DependencyContainer get() = data.dependencies
    override val swaps: SwapContainer get() = data.swaps
    override val replacements: ReplacementContainer get() = data.replacements
    override val filters: FilterContainer get() = data.filters

    internal val parent: Project get() = project.parent!!
    internal val controller: StonecutterControllerImpl get() = tree.getControllerImpl()
    internal val data: StonecutterBuildProperties get() = controller.getOrCreateParameters(project.hierarchy)

    init {
        configureProject()
    }

    private fun configureProject() = with(project) {
        plugins.apply("java")
        sourceSets.all {
            createProcessingTasks(this)
            this@StonecutterBuildImpl.tasks.configureSource(this)
        }
        this@StonecutterBuildImpl.tasks.registerNodeModelTask()
        configureTaskDependencies()
    }

    private fun configureTaskDependencies() = project.afterEvaluate {
        if (flags[StonecutterFlag.APPEND_SOURCES_AFTER_EVAL]) sourceSets.forEach(this@StonecutterBuildImpl.tasks::configureSource)
        if (flags[StonecutterFlag.GENERATE_SOURCES_ON_SYNC] && isIdeaSync) this@StonecutterBuildImpl.tasks.generate.keys.map { "$path:$it" }
            .let { gradle.requestTasks(it, path, projectDir) }

        controller.tasks.switchTaskProvider(current.project)?.configure {
            dependsOn(this@StonecutterBuildImpl.tasks.merge)
        }
    }

    private fun createProcessingTasks(src: SourceSet) {
        val prepareTask = tasks.registerPrepareTask(src) {
            sources.from(parent.files("src/${src.name}").asFileTree.filter { (filters as FilterContainerImpl).filter(it.toPath()) })
            root.set(parent.projectDirectory.resolve("src/${src.name}"))
            caches.set(tasks.processedCacheDir.resolve(src.name))
            project.provider { Json.encodeToString(data.apply { putDefaultReceiver() }.data) }
                .let(parameters::set)
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

    private fun putDefaultReceiver() {
        val version = dependencies.getOrDefault(flags[StonecutterFlag.IMPLICIT_RECEIVER], parse(current.version))
        dependencies[flags[StonecutterFlag.IMPLICIT_RECEIVER]] = version
        data.dependencies.delegate[""] = version
    }
}