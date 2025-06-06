package dev.kikugie.stonecutter.build

import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.dsl.*
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
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.property
import dev.kikugie.semver.data.Version as ParsedVersion

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
        val params = objects.property<String>()
            .value(provider { Json.encodeToString(data.convert(flags[StonecutterFlag.IMPLICIT_RECEIVER], parse(current.version))) })
            .apply(Property<String>::finalizeValueOnRead)
        sourceSets.all {
            createProcessingTasks(this, params)
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

    private fun createProcessingTasks(src: SourceSet, data: Provider<String>) {
        val overrides = project.projectDirectory.resolve("src/${src.name}")
        val prepareTask = tasks.registerPrepareTask(src) {
            params.set(data)
            parent.file("src/${src.name}").let(root::set)
            parent.fileTree("src/${src.name}").filter { (filters as FilterContainerImpl).filter(it.toPath()) }
                .let(source::setFrom)
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