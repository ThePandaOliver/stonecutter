package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.Version
import dev.kikugie.stitcher.data.replacement.ReplacementList
import kotlinx.serialization.Serializable

@Serializable
public data class StonecutterBuildData(
    val constants: Map<String, Boolean> = mutableMapOf(),
    val dependencies: Map<String, Version> = mutableMapOf(),
    val swaps: Map<String, String> = mutableMapOf(),
    val replacements: ReplacementList = ReplacementList(),
    val excludes: Set<String> = mutableSetOf(),
    val extensions: Set<String> = mutableSetOf(),
)
