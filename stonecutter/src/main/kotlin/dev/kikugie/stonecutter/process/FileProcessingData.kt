package dev.kikugie.stonecutter.process

import dev.kikugie.semver.VersionParser
import dev.kikugie.stitcher.data.replacement.*
import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.invoke
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

internal abstract class FileProcessingData {
    abstract val constants: MapProperty<Identifier, Boolean>
    abstract val swaps: MapProperty<Identifier, String>
    abstract val dependencies: MapProperty<Identifier, AnyVersion>
    abstract val replacements: ListProperty<ReplacementData>


    abstract class ReplacementData {
        abstract val type: Property<String>
        abstract val phase: Property<String>
        abstract val id: Property<String>
        abstract val sources: ListProperty<String>
        abstract val target: Property<String>

        private val actualPhase: ReplacementPhase
            get() = ReplacementPhase.valueOf(phase().uppercase())

        fun toActualReplacement(): Replacement = when(type()) {
            "REGEX" -> toRegexReplacement()
            "STRING" -> toStringReplacement()
            else -> error("Invalid replacement type: '$type'")
        }

        private fun toRegexReplacement(): RegexReplacement =
            RegexReplacement(sources().first().toRegex(), target(), actualPhase, id())

        private fun toStringReplacement(): StringReplacement =
            StringReplacement(sources().toMutableSet(), target(), actualPhase, id())
    }

    fun toActualParameters(): TransformParameters = TransformParameters(
        swaps(), constants(),
        dependencies().mapValues { VersionParser.parseLenient(it.value).value },
        replacements().map { it.toActualReplacement() }.run { ReplacementList(toMutableList()) }
    )
}
