package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.util.clearIfNotIncremental
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
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

public abstract class FileGeneratingTask : DefaultTask() {
    internal data class ExpectedFileState(val file: File, val source: File?)
    internal abstract class Action : WorkAction<Parameters> {
        override fun execute() = try {
            if (!parameters.source.isPresent) parameters.outputPath.deleteIfExists()
            else parameters.outputPath.let {
                it.parent.createDirectories()
                parameters.sourcePath.copyTo(it, overwrite = true)
            }
            Unit
        } catch (e: Exception) {
            throw RuntimeException("Failed to process file: [${e::class.qualifiedName}: ${e.message}", e)
        }
    }

    internal interface Parameters : WorkParameters {
        @get:Optional
        val source: RegularFileProperty
        val output: RegularFileProperty

        val sourcePath: Path get() = source.asFile().toPath()
        val outputPath: Path get() = output.asFile().toPath()
    }

    @get:Input
    public abstract val cache: Property<File>

    @get:Input
    public abstract val root: Property<File>

    @get:Input
    public abstract val source: Property<File>

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    public abstract val sources: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    public abstract val excludes: ConfigurableFileCollection

    @get:InputDirectory
    @get:Incremental
    public abstract val processed: DirectoryProperty

    @get:OutputDirectory
    public abstract val generated: DirectoryProperty

    @get:Inject
    public abstract val executor: WorkerExecutor

    @TaskAction
    public fun run(inputs: InputChanges) {
        inputs.clearIfNotIncremental(generated.asFile())
        val expected: List<ExpectedFileState> = buildExpectedFileStates(inputs)

        val queue = executor.noIsolation()
        for ((path, origin) in expected) queue.submit(Action::class) {
            output.set(generated.file(path.toString()))
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

        inputs.getFileChanges(processed).analyzeSource(cache(), ignored, this).let { (added, removed) ->
            ignored += added
            freed -= added
            freed += removed
        }

        inputs.getFileChanges(sources).analyzeSource(root(), ignored, this).let { (added, removed) ->
            ignored += added
            freed -= added
            freed += removed
        }

        for (it in freed) this += ExpectedFileState(it, null)
    }

    private fun Iterable<FileChange>.analyzeExcludes(collector: MutableList<ExpectedFileState>): Pair<MutableSet<File>, MutableSet<File>> {
        val ignoredPaths = mutableSetOf<File>()
        val freedPaths = mutableSetOf<File>()
        for (change in this) when {
            change.isExistingFile -> {
                val path = change.file.relativeTo(source())
                collector += ExpectedFileState(path, change.file)
                ignoredPaths += path
            }
            change.isRemovedFile -> {
                freedPaths += change.file.relativeTo(source())
            }
        }

        return ignoredPaths to freedPaths
    }

    private fun Iterable<FileChange>.analyzeSource(root: File, ignored: Set<File>, collector: MutableList<ExpectedFileState>): Pair<MutableSet<File>, MutableSet<File>> {
        val addedPaths = mutableSetOf<File>()
        val freedPaths = mutableSetOf<File>()

        for (change in this) when {
            change.file.relativeTo(root) in ignored -> continue
            change.isExistingFile -> {
                val path = change.file.relativeTo(root)
                collector += ExpectedFileState(path, change.file)
                addedPaths += path
            }
            change.isRemovedFile -> {
                freedPaths += change.file.relativeTo(root)
            }
        }

        return addedPaths to freedPaths
    }
}