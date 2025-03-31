package dev.kikugie.stonecutter.process

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class FileMergingTask : DefaultTask() {
    /**
     * Sources directory to copy unprocessed files from, matching [FileProcessingTask.sources].
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /**
     * Sources directory under `versions/{dest version}/`,
     * specifying, which files from [sources] shouldn't be copied over.
     */
    @get:Optional
    @get:InputDirectory
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val overrides: DirectoryProperty

    /**
     * Processed files directory matching [FileProcessingTask.caches].
     */
    @get:InputDirectory
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val caches: DirectoryProperty

    /**
     * Output directory for the files, which is either
     * [sources] or generated sources directory.
     */
    @get:OutputDirectory
    abstract val results: DirectoryProperty

    /**
     * Concurrent executor for the file processing.
     */
    @get:Inject
    abstract val executor: WorkerExecutor

    @TaskAction
    fun run() {
        val queue = executor.noIsolation()
        sources.asFileTree.visit {
            if (!isDirectory) enqueueFile(queue)
        }
        queue.await()
    }

    private fun FileVisitDetails.enqueueFile(queue: WorkQueue) = queue.submit(FileMergingAction::class) {
        source.set(file)
        cache.set(caches.file(relativePath.pathString))
        if (overrides.isPresent) override
            .set(overrides.file(relativePath.pathString))
        output.set(results.file(relativePath.pathString))
    }
}