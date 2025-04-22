package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import dev.kikugie.stonecutter.util.*
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File
import kotlin.reflect.KClass

internal class StonecutterBuildTasksImpl(private val ext: StonecutterBuildImpl) : StonecutterBuildTasks {
    override val prepare: MutableTaskProviderMap<FileProcessingTask> = mutableMapOf()
    override val generate: MutableTaskProviderMap<FileGeneratingTask> = mutableMapOf()
    override val merge: MutableTaskProviderMap<Copy> = mutableMapOf()
    override val processedCacheDir: File get() = ext.project.buildDirectory.resolve("stonecutter-cache/sources")
    override val generatedSourcesDir: File get() = ext.project.buildDirectory.resolve("generated/stonecutter")
    private val registeredSources: MutableSet<File> = mutableSetOf()
    private val logger by logger("StonecutterBuildTasks")

    fun registerPrepareTask(src: SourceSet, config: FileProcessingTask.() -> Unit) : TaskProvider<FileProcessingTask> =
        registerDefaultTask(prepareTaskName(src), FileProcessingTask::class).apply { configure(config); prepare[name] = this }

    fun registerGenerateTask(src: SourceSet, config: FileGeneratingTask.() -> Unit) : TaskProvider<FileGeneratingTask> =
        registerDefaultTask(generateTaskName(src), FileGeneratingTask::class).apply { configure(config); generate[name] = this }

    fun registerMergeTask(src: SourceSet, config: Copy.() -> Unit) : TaskProvider<Copy> =
        registerDefaultTask(mergeTaskName(src), Copy::class).apply { configure(config); merge[name] = this }

    private fun <T : Task> registerDefaultTask(name: String, cls: KClass<T>): TaskProvider<T> = ext.project.tasks.register(name, cls) {
        group = "stonecutter-impl"
        description = "Internal Stonecutter task. Do not call manually."
    }

    override fun configureSource(src: SourceSet) {
        val branchSrc: File = ext.parent.projectDirectory.resolve("src")
        val versionSrc: File = ext.project.projectDirectory.resolve("src")
        for (set in src.allSources()) {
            val matchingDirs = set.sourceDirectories
                .map { it.relativeTo(versionSrc) }
                .filterNot { it.startsWith("..") }

            if (ext.current.isActive) applyDirectories(set, matchingDirs, branchSrc, null, true)
            applyDirectories(set, matchingDirs, generatedSourcesDir, generateTaskName(src), !ext.current.isActive)
        }
    }

    private fun applyDirectories(set: SourceDirectorySet, matching: Iterable<File>, root: File, task: String?, apply: Boolean): List<File> {
        val dirs = matching.map(root::resolve).filterNot(registeredSources::contains).ifEmpty { return emptyList() }
        if (apply) set.srcDir(ext.project.files(dirs).apply { if (task != null) builtBy("${ext.project.path}:$task") })
        registeredSources += dirs
        return dirs
    }
}