package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stitcher.data.replacement.ReplacementList
import kotlinx.serialization.Serializable

@Serializable
public data class StonecutterBuildData(
    val constants: Map<String, Boolean> = mutableMapOf(),
    val dependencies: Map<String, ParsedVersion> = mutableMapOf(),
    val swaps: Map<String, String> = mutableMapOf(),
    val replacements: ReplacementList = ReplacementList(),
    val excludes: Set<String> = mutableSetOf(),
    val extensions: Set<String> = DEFAULT_EXTENSIONS.toMutableSet(),
) {
    public companion object {
        @JvmField
        public val DEFAULT_EXTENSIONS: Set<String> = setOf("java", "kt", "kts", "groovy", "gradle", "scala", "sc", "json5", "hjson")
    }
}
