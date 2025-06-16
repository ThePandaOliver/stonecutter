package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.BuildPropertiesContainer
import dev.kikugie.stonecutter.data.container.ProjectNodeContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.dsl.*
import dev.kikugie.stonecutter.data.dsl.impl.DependencyContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.util.*
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.kotlin.dsl.the
import dev.kikugie.semver.data.Version as ParsedVersion

internal open class StonecutterBuildImpl(val project: Project) :
    StonecutterBuildExtension, VersionOperations<ParsedVersion> by LenientOperations {
    internal val parent: Project get() = project.parent!!
    internal val properties: StonecutterBuildProperties by lazy { project.gradle.getContainer<BuildPropertiesContainer>()[node] }

    override val node: ProjectNode by lazy {
        val container = project.gradle.getContainer<ProjectNodeContainer>()
        checkNotNull(container[project]) { "${project.hierarchy} is not a registered Stonecutter node" }
    }
    override val tasks: StonecutterBuildTasksImpl = StonecutterBuildTasksImpl(this)
    override val flags: FlagContainer by lazy { tree.project.the<StonecutterControllerExtension>().flags }

    override val constants: ConstantContainer get() = properties.constants
    override val dependencies: DependencyContainer get() = properties.dependencies
    override val swaps: SwapContainer get() = properties.swaps
    override val replacements: ReplacementContainer get() = properties.replacements
    override val filters: PatternFilterable get() = properties.filters

    init {
        configureProject()
    }

    private fun configureProject() = with(project) {
        plugins.apply("java")
        sourceSets.all {
            createProcessingTasks(this)
            this@StonecutterBuildImpl.tasks.configureSource(this)
        }
        filters.include("**/*.java", "**/*.kt", "**/*.kts", "**/*.groovy", "**/*.gradle", "**/*.scala", "**/*.sc", "**/*.json5", "**/*.hjson")
        this@StonecutterBuildImpl.tasks.registerNodeModelTask()
        configureTaskDependencies()
        afterEvaluate {
            val deps = this@StonecutterBuildImpl.properties.dependencies as DependencyContainerImpl
            val default = deps.getOrDefault(flags[StonecutterFlag.IMPLICIT_RECEIVER], current.version)
            deps[flags[StonecutterFlag.IMPLICIT_RECEIVER]] = default
            deps.property[""] = default
        }
    }

    private fun configureTaskDependencies() = project.afterEvaluate {
        if (flags[StonecutterFlag.APPEND_SOURCES_AFTER_EVAL]) sourceSets.forEach(this@StonecutterBuildImpl.tasks::configureSource)
        if (flags[StonecutterFlag.GENERATE_SOURCES_ON_SYNC] && isIdeaSync) this@StonecutterBuildImpl.tasks.generate.keys.map { "$path:$it" }
            .let { gradle.requestTasks(it, path, projectDir) }
    }

    private fun createProcessingTasks(src: SourceSet) {
        val overrides = project.projectDirectory.resolve("src/${src.name}")
        val prepareTask = tasks.registerPrepareTask(src) {
            params.set(properties.data)
            parent.file("src/${src.name}").let(root::set)
            project.provider { parent.fileTree("src/${src.name}").matching(filters) }.let { source.setFrom(it) }
            tasks.processedCacheDir.resolve(src.name).let(destination::set)
        }

        tasks.registerGenerateTask(src) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            from(parent.projectDirectory.resolve("src/${src.name}"), tasks.processedCacheDir.resolve(src.name))
            exclude { !it.isDirectory && it.relativePath.getFile(overrides).exists() }
            into(tasks.generatedSourcesDir.resolve(src.name))
            dependsOn(prepareTask)
        }

        tasks.registerMergeTask(src) {
            from(tasks.processedCacheDir.resolve(src.name))
            into(parent.projectDirectory.resolve("src/${src.name}"))
            dependsOn(prepareTask)
        }
    }
}