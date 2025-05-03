package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.util.clearIfNotIncremental
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit
import org.gradle.work.*
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * Processes files that have versioned comments from [sources] into the [caches] directory.
 * This allows simply copying them recursively for version switching and generating sources.
 */
public abstract class FileProcessingTask : DefaultTask() {
    /**
     * Base directory for [sources] to determine the relative location in [caches].
     */
    @get:Input
    public abstract val root: Property<File>
    /**
     * Source directory for the files to be processed,
     * which should be the `src` directory in the branch root.
     */
    @get:InputFiles
    @get:Incremental
    @get:IgnoreEmptyDirectories
    public abstract val sources: ConfigurableFileCollection

    /**
     * Processor parameters used for file processing.
     */
    @get:Input
    public abstract val parameters: Property<String>

    /**
     * Cache directory for processed files,
     * which is under `versions/{dest version}/build/stonecutter-cache/sources/`
     */
    @get:OutputDirectory
    public abstract val caches: DirectoryProperty

    @get:Inject
    public abstract val executor: WorkerExecutor

    @TaskAction
    public fun run(inputs: InputChanges) {
        inputs.clearIfNotIncremental(caches.asFile())
        val queue = executor.noIsolation()

        for (change in inputs.getFileChanges(sources))
            if (change.fileType != FileType.DIRECTORY)
                queue.processFile(change)
        queue.await()
    }

    private fun WorkQueue.processFile(change: FileChange) = when (change.changeType) {
        ChangeType.REMOVED -> processDeleted(change)
        else -> processModified(change)
    }

    private fun WorkQueue.processDeleted(change: FileChange) = submit(FileProcessingAction::class) {
        output.set(change.file.cacheFile())
    }

    private fun WorkQueue.processModified(change: FileChange) = submit(FileProcessingAction::class) {
        source.set(change.file)
        output.set(change.file.cacheFile())
        transform.set(parameters)
    }

    private fun File.fromRoot() = relativeTo(root())
    private fun File.cacheFile() = caches().asFile.resolve(fromRoot())
}