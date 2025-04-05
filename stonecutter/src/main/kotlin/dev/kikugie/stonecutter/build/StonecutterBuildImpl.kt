package dev.kikugie.stonecutter.build

import dev.kikugie.stitcher.data.replacement.RegexReplacement
import dev.kikugie.stitcher.data.replacement.StringReplacement
import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.getChecked
import dev.kikugie.stonecutter.keysToString
import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileMergingTask
import dev.kikugie.stonecutter.process.FileProcessingData
import dev.kikugie.stonecutter.process.FileProcessingData.ReplacementData
import dev.kikugie.stonecutter.process.FileProcessingTask
import dev.kikugie.stonecutter.sourceSets
import kotlinx.coroutines.yield
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.DefaultTaskExecutionRequest
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name

private fun SourceSet.allSources() = sequence {
    yield(java)
    yield(resources)
    extensions.extensionsSchema.asSequence()
        .mapNotNull { extensions[it.name] }
        .filterIsInstance<SourceDirectorySet>()
        .let { yieldAll(it) }
}

private fun Project.findProjectTree(): ProjectTree {
    val project = checkNotNull(parent) { "Stonecutter plugin was incorrectly applied. Refer to the documentation for more info."}
        .let { it.parent ?: it }
    val container = gradle.getContainer<ProjectTreeContainer>()
    return checkNotNull(container[project]) { "Tree for '${project.hierarchy}' not found in ${container.projects.keysToString()}" }
}

private fun ProjectTree.getControllerImpl(): StonecutterControllerImpl =
    project.extensions.getByType<StonecutterControllerExtension>() as StonecutterControllerImpl

private fun String.upFirst() = replaceFirstChar(Char::uppercase)

internal open class StonecutterBuildImpl @JvmOverloads constructor (
    val project: Project,
    final override val tree: ProjectTree = project.findProjectTree(),
    val controller: StonecutterControllerImpl = tree.getControllerImpl()
) : StonecutterBuildExtension, StonecutterBuildParams by controller.getOrCreateParameters(project.hierarchy) {
    private val parent get() = project.parent!!
    final override val branch: ProjectBranch = tree.getChecked(project.parent!!.hierarchy) {
        "Branch for '$it' not found in ${tree.hierarchy}: ${keysToString()}"
    }
    final override val node: ProjectNode = branch.getChecked(project.hierarchy) {
        "Node for '$it' not found in ${branch.hierarchy}: ${keysToString()}"
    }

    init {
        configureProject()
    }

    private fun configureProject() = with(project) {
        plugins.apply("java")
        val prepareTasks: MutableList<String> = mutableListOf()

        sourceSets.all {
            prepareTasks += allSources()
                .map { configureSourceDirectory(this, it) }
                .map { "$path:${it.name}" }
        }

        configureTaskDependencies(prepareTasks)
    }

    private fun configureTaskDependencies(prepareTasks: MutableList<String>) = project.afterEvaluate {
        if (System.getProperty("idea.sync.active", "false").toBoolean()) gradle.startParameter.run {
            val requests = taskRequests + DefaultTaskExecutionRequest(prepareTasks, path, projectDir)
            setTaskRequests(requests)
        }
    }

    private fun configureSourceDirectory(set: SourceSet, dir: SourceDirectorySet): TaskProvider<FileGeneratingTask> {
        val currentDirectories = dir.sourceDirectories.files.toSet()
        val processedDirectories = mapAndAttachSources(if (SourceSet.isMain(set)) "" else set.name, dir)
        val pathSuffix = "${set.name}/${dir.name}"
        val taskSuffix = buildString {
            if (!SourceSet.isMain(set))
                append(set.name.upFirst())
            append(dir.name.upFirst())
        }

        val prepareTask = project.tasks.register<FileProcessingTask>("stonecutterPrepare$taskSuffix") {
            description = "Internal Stonecutter task. Do not call manually."

            sources.from(processedDirectories)
            caches.set(project.layout.buildDirectory.dir("stonecutter-cache/sources/$pathSuffix"))
            parameters.set(createProcessingData())
        }

        return project.tasks.register<FileGeneratingTask>("stonecutterGenerate$taskSuffix") {
            description = "Internal Stonecutter task. Do not call manually."

            sources.from(processedDirectories)
            excludes.from(currentDirectories)
            processed.set(project.layout.buildDirectory.dir("stonecutter-cache/sources/$pathSuffix"))
            generated.set(project.layout.buildDirectory.dir("generated/stonecutter/$pathSuffix"))

            dependsOn(prepareTask)
        }
    }

    /**
     * Sources under `versions/./src/` are replaced with root's `src/` for the active version.
     * Matched root sources are returned to be registered in [registerPrepareSourcesTask].
     */
    private fun mapAndAttachSources(name: String, dir: SourceDirectorySet): List<File> {
        val root = parent.layout.projectDirectory.dir("src").asFile
        val src = project.layout.projectDirectory.dir("src").asFile
        val (match, nomatch) = dir.sourceDirectories.groupBy { it.startsWith(src) }
            .let { it.getOrDefault(true, emptyList()) to it.getOrDefault(false, emptyList()) }
        validateSourceConsistency(dir, src, match.toSet())
        val remapped = match.map { root.resolve(it.relativeTo(src)) }
        if (current.isActive) dir.setSrcDirs(remapped + nomatch)
        else for (it in match) project.layout.buildDirectory
            .dir("generated/stonecutter/${it.relativeTo(src)}")
            .let { project.files(it).builtBy("${project.path}:stonecutterPrepare${name.upFirst()}${dir.name.upFirst()}") }
            .let(dir::srcDir)

        return remapped
    }

    private fun validateSourceConsistency(dir: SourceDirectorySet, src: File, registered: Set<File>) = project.afterEvaluate {
        val new = dir.sourceDirectories.filter { it.startsWith(src) && it !in registered }.toSet()
        if (new.isNotEmpty()) """
            The following source directories were added after Stonecutter was configured and won't be processed:
            ${new.joinToString("\n") { "  - $it" }}
        """.trimIndent().let(logger::warn)
    }

    private fun createProcessingData(): FileProcessingData = project.objects.newInstance<FileProcessingData>().apply {
        val providers = project.providers
        val objects = project.objects

        constants.set(providers.provider { consts })
        swaps.set(providers.provider { this@StonecutterBuildImpl.swaps })
        dependencies.set(providers.provider { dependenciesWithDefault("minecraft", current.version) })
        replacements.set(providers.provider { this@StonecutterBuildImpl.replacements.map {
            objects.newInstance<ReplacementData>().apply {
                phase.set(it.phase.name); it.identifier?.let(id::set)
                if (it is RegexReplacement) {
                    type.set("REGEX"); sources.set(listOf(it.pattern.pattern)); target.set(it.target)
                }
                else if (it is StringReplacement) {
                    type.set("STRING"); sources.set(it.sources); target.set(it.target)
                }
            }
        } })
    }

    private fun dependenciesWithDefault(key: String, version: AnyVersion) = buildMap<String, String> {
        this += dependencies.mapValues { (_, it) -> it.toString() }
        getOrDefault(key, version).let { this[key] = it; this[""] = it }
    }
}