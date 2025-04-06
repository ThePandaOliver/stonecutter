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
import javax.inject.Inject

internal abstract class FileProcessingTask : DefaultTask() {
    /**
     * Source directory for the files to be processed,
     * which should be the `src` directory in the branch root.
     */
    @get:InputFiles
    @get:Incremental
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /**
     * Processor parameters used for file processing.
     */
    @get:Input
    abstract val parameters: Property<FileProcessingData>

    /**
     * Cache directory for processed files,
     * which should be `versions/{dest version}/build/stonecutter-cache/sources/`
     * or `/build/stonecutter-cache/oob/{dest version}/sources/` in the branch root.
     */
    @get:OutputDirectory
    abstract val caches: DirectoryProperty

    /**
     * Concurrent executor for the file processing.
     */
    @get:Inject
    abstract val executor: WorkerExecutor

    @TaskAction
    fun run(inputs: InputChanges) {
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
        output.set(change.normalizedPath.tempFile())
    }

    private fun WorkQueue.processModified(change: FileChange) = submit(FileProcessingAction::class) {
        source.set(change.file)
        output.set(change.normalizedPath.tempFile())
        transform.set(parameters)
    }

    private fun String.tempFile() = caches().asFile.resolve(this)
}