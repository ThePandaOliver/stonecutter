package dev.kikugie.stonecutter.process

import dev.kikugie.stitcher.data.replacement.ReplacementExecutor.Companion.replaceWithScannedTokens
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stitcher.data.token.ContentType
import dev.kikugie.stitcher.eval.join
import dev.kikugie.stitcher.exception.ErrorHandler
import dev.kikugie.stitcher.exception.StoringErrorHandler
import dev.kikugie.stitcher.exception.join
import dev.kikugie.stitcher.parser.FileParser
import dev.kikugie.stitcher.scanner.CommentRecognizers
import dev.kikugie.stitcher.scanner.Scanner
import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.stitcher.transformer.Transformer
import dev.kikugie.stonecutter.util.clearIfNotIncremental
import dev.kikugie.stonecutter.util.execute
import dev.kikugie.stonecutter.util.invoke
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import kotlin.io.path.*

public abstract class SCPrepareTask : DefaultTask() {
    @get:Input
    public abstract val params: Property<String>

    @get:Input
    public abstract val root: Property<File>

    @get:[InputFiles Incremental IgnoreEmptyDirectories]
    public abstract val source: ConfigurableFileCollection

    @get:OutputDirectory
    public abstract val destination: DirectoryProperty

    @get:Inject
    public abstract val executor: WorkerExecutor

    @TaskAction
    public fun run(inputs: InputChanges) {
        inputs.clearIfNotIncremental(destination.asFile())
        executor.execute {
            for (change in inputs.getFileChanges(source))
                if (change.fileType != FileType.DIRECTORY) it.processFile(change)
        }
    }

    private fun WorkQueue.processFile(change: FileChange) = submit(SCPrepareAction::class) {
        params.set(this@SCPrepareTask.params)
        source.set(change.file)
        output.set(change.file.cacheFile())
    }

    private fun File.cacheFile(): File = destination.asFile().resolve(relativeTo(root()))
}

private interface SCPrepareAction : WorkAction<SCPrepareAction.Parameters> {
    interface Parameters : WorkParameters {
        val params: Property<String>
        val source: RegularFileProperty
        val output: RegularFileProperty
    }

    override fun execute() {
        val source: Path = parameters.source.asFile().toPath()
        val output: Path = parameters.output.asFile().toPath()

        if (!source.exists()) { output.deleteIfExists(); return }
        val transforms: TransformParameters = Json.decodeFromString(parameters.params())

        val original: CharSequence = source.readText(Charsets.UTF_8)
        val modified: CharSequence = original
            .applyReplacements(transforms, ReplacementPhase.FIRST)
            .applyTransformation(transforms)
            .applyReplacements(transforms, ReplacementPhase.LAST)
        if (original == modified) { output.deleteIfExists(); return }

        output.parent.createDirectories()
        output.writeText(modified, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    private fun CharSequence.applyReplacements(transforms: TransformParameters, phase: ReplacementPhase): CharSequence =
        replaceWithScannedTokens(transforms.replacements, phase, CommentRecognizers.DEFAULT)

    private fun CharSequence.applyTransformation(transforms: TransformParameters): CharSequence {
        val handler: ErrorHandler = StoringErrorHandler()
        val parser: FileParser = FileParser.create(this, handler, CommentRecognizers.DEFAULT, transforms)
        val ast = parser.parse()
        handler.throwIfHasErrors()
        Transformer(ast, CommentRecognizers.DEFAULT, transforms, handler).process()
        handler.throwIfHasErrors()
        return ast.join()
    }

    private fun ErrorHandler.throwIfHasErrors(): Nothing? {
        if (errors.isEmpty()) return null
        for (err in errors)
            System.err.println(err.join())
        val file = parameters.source().asFile.absolutePath
        throw RuntimeException("Failed to parse $file")
    }
}