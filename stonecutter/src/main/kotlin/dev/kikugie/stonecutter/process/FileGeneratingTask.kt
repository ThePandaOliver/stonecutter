package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.util.clearIfNotIncremental
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

internal abstract class FileGeneratingTask : DefaultTask() {
    data class ExpectedFileState(val file: String, val source: File?)
    abstract class Action : WorkAction<Parameters> {
        override fun execute() = try {
            if (!parameters.source.isPresent) parameters.outputPath.deleteIfExists()
            else parameters.outputPath.let {
                it.parent.createDirectories()
                parameters.sourcePath.copyTo(it, overwrite = true)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to process file: [${e::class.qualifiedName}: ${e.message}", e)
        }.let { }
    }

    interface Parameters : WorkParameters {
        @get:Optional
        val source: RegularFileProperty
        val output: RegularFileProperty

        val sourcePath: Path get() = source.asFile().toPath()
        val outputPath: Path get() = output.asFile().toPath()
    }

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val excludes: ConfigurableFileCollection

    @get:InputDirectory
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val processed: DirectoryProperty

    @get:OutputDirectory
    abstract val generated: DirectoryProperty

    @get:Inject
    abstract val executor: WorkerExecutor

    @TaskAction
    fun run(inputs: InputChanges) {
        inputs.clearIfNotIncremental(generated.asFile())
        val expected: List<ExpectedFileState> = buildExpectedFileStates(inputs)

        val queue = executor.noIsolation()
        for ((path, origin) in expected) queue.submit(Action::class) {
            output.set(generated.file(path))
            origin?.let(source::set)
        }
    }

    private val FileChange.isExistingFile
        get() = changeType != ChangeType.REMOVED && fileType == FileType.FILE
    private val FileChange.isRemovedFile
        get() = changeType == ChangeType.REMOVED && fileType != FileType.DIRECTORY

    private fun buildExpectedFileStates(inputs: InputChanges): List<ExpectedFileState> = buildList {
        val (ignored, freed) = inputs.getFileChanges(excludes)
            .analyzeExcludes(this)

        inputs.getFileChanges(processed).analyzeSource(ignored, this).let { (added, removed) ->
            ignored += added
            freed -= added
            freed += removed
        }

        inputs.getFileChanges(sources).analyzeSource(ignored, this).let { (added, removed) ->
            ignored += added
            freed -= added
            freed += removed
        }

        for (it in freed) this += ExpectedFileState(it, null)
    }

    private fun Iterable<FileChange>.analyzeExcludes(collector: MutableList<ExpectedFileState>): Pair<MutableSet<String>, MutableSet<String>> {
        val ignoredPaths = mutableSetOf<String>()
        val freedPaths = mutableSetOf<String>()
        for (change in this) when {
            change.isExistingFile -> {
                collector += ExpectedFileState(change.normalizedPath, change.file)
                ignoredPaths += change.normalizedPath
            }
            change.isRemovedFile -> {
                freedPaths += change.normalizedPath
            }
        }

        return ignoredPaths to freedPaths
    }

    private fun Iterable<FileChange>.analyzeSource(ignored: Set<String>, collector: MutableList<ExpectedFileState>): Pair<MutableSet<String>, MutableSet<String>> {
        val addedPaths = mutableSetOf<String>()
        val freedPaths = mutableSetOf<String>()

        for (change in this) when {
            change.normalizedPath in ignored -> continue
            change.isExistingFile -> {
                collector += ExpectedFileState(change.normalizedPath, change.file)
                addedPaths += change.normalizedPath
            }
            change.isRemovedFile -> {
                freedPaths += change.normalizedPath
            }
        }

        return addedPaths to freedPaths
    }
}