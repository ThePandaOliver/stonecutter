package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.build.param.StonecutterBuildParams
import dev.kikugie.stonecutter.build.task.StonecutterBuildInternalTasks
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.controller.flag.GENERATE_SOURCES_ON_SYNC
import dev.kikugie.stonecutter.controller.flag.IMPLICIT_RECEIVER
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.util.*
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal open class StonecutterBuildImpl @JvmOverloads constructor(
    val project: Project,
    final override val tree: ProjectTree = project.findProjectTree(),
    val controller: StonecutterControllerImpl = tree.getControllerImpl(),
    val data: StonecutterBuildData = controller.getOrCreateParameters(project.hierarchy),
) : StonecutterBuildExtension, StonecutterBuildParams by data {
    private val parent get() = project.parent!!
    internal val internals = StonecutterBuildInternalTasks()
    final override val branch: ProjectBranch = tree.getChecked(project.parent!!.hierarchy) {
        "Branch for '$it' not found in ${tree.hierarchy}: ${keysToString()}"
    }
    final override val node: ProjectNode = branch.getChecked(project.hierarchy) {
        "Node for '$it' not found in ${branch.hierarchy}: ${keysToString()}"
    }
    override val flags: FlagContainer
        get() = controller.flags

    init {
        configureProject()
    }

    private fun configureProject() = with(project) {
        plugins.apply("java")
        sourceSets.all {
            for (it in allSources()) configureSourceDirectory(this, it)
        }
        configureTaskDependencies()
    }

    private fun configureTaskDependencies() = project.afterEvaluate {
        if (flags[GENERATE_SOURCES_ON_SYNC] && isIdeaSync) internals.generateTasks.map { "$path:${it.name}" }
            .let { gradle.requestTasks(it, path, projectDir) }

        controller.internals.switchTaskProvider(current.project)?.configure {
            dependsOn(internals.mergeTasks)
        }

        with(filter) {
            for (it in internals.prepareTasks) it.configure {
                sources.from(sources.asFileTree.filter { filter(it.toPath()) })
            }
        }
    }

    private fun configureSourceDirectory(set: SourceSet, dir: SourceDirectorySet): TaskProvider<*> {
        val pathSuffix = "${set.name}/${dir.name}"
        val currentDirectories = dir.sourceDirectories.files.toSet()
        val processedDirectories = mapAndAttachSources(set, dir)
        val cacheDirectory = project.layout.buildDirectory.dir("stonecutter-cache/sources/$pathSuffix")
        val generatedDirectory = project.layout.buildDirectory.dir("generated/stonecutter/$pathSuffix")

        val prepareTask = internals.registerPrepareTask(project, set, dir) {
            description = "Internal Stonecutter task. Do not call manually."

            sources.from(processedDirectories)
            caches.set(cacheDirectory)
            parameters.set(project.provider { data.asProcessingData(flags[IMPLICIT_RECEIVER], current.version) })
        }

        internals.registerMergeTask(project, set, dir) {
            description = "Internal Stonecutter task. Do not call manually."

            from(cacheDirectory)
            into(parent.layout.projectDirectory.dir("src/$pathSuffix"))
            dependsOn(prepareTask)
        }

        return internals.registerGenerateTask(project, set, dir) {
            description = "Internal Stonecutter task. Do not call manually."

            sources.from(processedDirectories)
            excludes.from(currentDirectories)
            processed.set(cacheDirectory)
            generated.set(generatedDirectory)

            dependsOn(prepareTask)
        }
    }

    /**
     * Sources under `versions/./src/` are replaced with root's `src/` for the active version.
     * Matched root sources are returned to be registered in [registerPrepareSourcesTask].
     */
    private fun mapAndAttachSources(set: SourceSet, dir: SourceDirectorySet): List<File> {
        val root = parent.layout.projectDirectory.dir("src").asFile
        val src = project.layout.projectDirectory.dir("src").asFile
        val match = dir.sourceDirectories.filter { it.startsWith(src) }
        validateSourceConsistency(dir, src, match.toSet())
        val remapped = match.map { root.resolve(it.relativeTo(src)) }
        if (current.isActive) dir.srcDirs(project.files(remapped))
        else for (it in match) project.layout.buildDirectory
            .dir("generated/stonecutter/${it.relativeTo(src)}")
            .let { project.files(it).builtBy("${project.path}:${internals.prepareTaskName(set, dir)}") }
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
}