@file:UseSerializers(RegexSerializer::class)

package dev.kikugie.stonecutter.data.parameters

import dev.kikugie.semver.Version
import dev.kikugie.semver.VersionParser
import dev.kikugie.stitcher.data.replacement.ReplacementList
import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.stitcher.util.RegexSerializer
import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.build.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString

/**
 * Represents the build parameters used by the file processor.
 *
 * @property constants Constant values set by [ConstantVariants]
 * @property swaps Swap replacements set by [SwapVariants]
 * @property dependencies Dependency versions set by [DependencyVariants]
 * @property replacements String and regex replacement entries set by [ReplacementVariants]
 * @property extensions Set of file formats allowlisted for file processing by [FilterVariants]
 * @property exclusions Set of individual files excluded from processing by [FilterVariants]
 */
@Serializable
public data class BuildParameters(
    val constants: MutableMap<Identifier, Boolean> = mutableMapOf(),
    val swaps: MutableMap<Identifier, String> = mutableMapOf(),
    val dependencies: MutableMap<Identifier, Version> = mutableMapOf(),
    val replacements: ReplacementList = ReplacementList(),
    val extensions: MutableSet<String> = mutableSetOf("java", "kt", "kts", "groovy", "gradle", "scala", "sc", "json5", "hjson"),
    val exclusions: MutableSet<String> = mutableSetOf()
) {
    /**
     * Checks if the given [file] is valid according to allowed [extensions] and [exclusions].
     * The project switching task expects files in [exclusions] to start at project's `src/` directory,
     * so the given path should be relativised accordingly.
     */
    public fun checkFile(file: Path): Boolean = file.invariantSeparatorsPathString
        .let { path -> file.extension in extensions && path !in exclusions }

    /**
     * Converts required data to the format required by the file transformer.
     * Parameter [key] specifies the name of the implicit version receiver
     * (usually `minecraft`, which makes the following checks equal: `if >1.21` and `if minecraft: >1.21`,
     * but can be overridden by [dev.kikugie.stonecutter.controller.StonecutterController.defaultReceiver]).
     * Parameter [version] specifies the version for the given [key].
     */
    public fun toTransformParameters(version: AnyVersion, key: String): TransformParameters = with(dependencies) {
        getOrElse(key) { VersionParser.parseLenient(version).value }.let {
            put(key, it)
            put("", it)
        }
        TransformParameters(swaps, constants, this, replacements)
    }
}