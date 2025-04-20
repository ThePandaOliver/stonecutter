package dev.kikugie.stonecutter.process

import dev.kikugie.semver.LenientVersionOperations
import dev.kikugie.stitcher.data.replacement.*
import dev.kikugie.stitcher.data.replacement.ReplacementExecutor.Companion.replaceWithScannedTokens
import dev.kikugie.stitcher.data.token.ContentType
import dev.kikugie.stitcher.data.token.Token
import dev.kikugie.stitcher.eval.join
import dev.kikugie.stitcher.exception.ErrorHandler
import dev.kikugie.stitcher.exception.StoringErrorHandler
import dev.kikugie.stitcher.exception.join
import dev.kikugie.stitcher.parser.FileParser
import dev.kikugie.stitcher.scanner.CommentRecognizers
import dev.kikugie.stitcher.scanner.Scanner
import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.stitcher.transformer.Transformer
import dev.kikugie.stonecutter.process.FileProcessingData.ReplacementData
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

internal abstract class FileProcessingAction : WorkAction<FileProcessingAction.Parameters> {
    interface Parameters : WorkParameters {
        val transform: Property<FileProcessingData>
        val source: RegularFileProperty
        val output: RegularFileProperty

        val sourcePath: Path get() = source.get().asFile.toPath()
        val outputPath: Path get() = output.get().asFile.toPath()
        fun readSource() = source.get().asFile.readText(Charsets.UTF_8)
    }

    private val transforms: TransformParameters by lazy { parameters.transform().toActualParameters() }

    override fun execute() {
        if (parameters.source.isPresent) processFile()
        else deleteFile()
    }

    private fun deleteFile() {
        parameters.output().asFile.toPath().deleteIfExists()
    }

    private fun processFile() {
        val original: CharSequence = parameters.readSource()
        var transformed: CharSequence = original
            .applyReplacements(ReplacementPhase.FIRST)

        if (transformed.isPlain()) {
            parameters.outputPath.deleteIfExists()
            return
        }

        transformed = transformed
            .applyTransformation()
            .applyReplacements(ReplacementPhase.LAST)

        parameters.outputPath.parent.createDirectories()
        if (original == transformed) parameters.sourcePath
            .copyTo(parameters.outputPath, overwrite = true)
        else parameters.outputPath
            .writeText(transformed, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    private fun CharSequence.applyReplacements(phase: ReplacementPhase): CharSequence =
        replaceWithScannedTokens(transforms.replacements, phase, CommentRecognizers.DEFAULT)

    private fun CharSequence.applyTransformation(): CharSequence {
        val handler: ErrorHandler = StoringErrorHandler()
        val parser: FileParser = FileParser.create(this, handler, CommentRecognizers.DEFAULT, transforms)
        val ast = parser.parse()
        handler.throwIfHasErrors()
        Transformer(ast, CommentRecognizers.DEFAULT, transforms, handler).process()
        handler.throwIfHasErrors()
        return ast.join()
    }

    private fun CharSequence.isPlain(): Boolean {
        val tokens: Iterable<Token> = Scanner.scan(this, CommentRecognizers.DEFAULT)
        return tokens.none {
            it.value.isNotEmpty() && it.type == ContentType.COMMENT && when(it.value.first()) {
                '?', '$', '~' -> true
                else -> false
            }
        }
    }

    private fun ErrorHandler.throwIfHasErrors(): Nothing? {
        if (errors.isEmpty()) return null
        val file = parameters.source().asFile.absolutePath
        throw RuntimeException("Failed to parse $file").apply {
            errors.forEach { addSuppressed(RuntimeException(it.join())) }
        }
    }

    private fun FileProcessingData.toActualParameters(): TransformParameters = TransformParameters(
        swaps(),
        constants(),
        dependencies().mapValues { (_, it) -> LenientVersionOperations.parseVersion(it).getOrThrow() },
        replacements().map { it.toActualReplacement() }.run { ReplacementList(toMutableList()) }
    )

    private fun ReplacementData.toActualReplacement(): Replacement = when(type()) {
        "REGEX" -> RegexReplacement(sources().first().toRegex(), target(), ReplacementPhase.valueOf(phase()), id.orNull)
        "STRING" -> StringReplacement(sources().toMutableSet(), target(), ReplacementPhase.valueOf(phase()), id.orNull)
        else -> error("Unknown replacement type: $type")
    }
}