package dev.kikugie.stwotcher.data.param

import dev.kikugie.semver.Version
import dev.kikugie.stitcher.data.replacement.ReplacementList
import kotlinx.serialization.Serializable

@Serializable
data class ProcessParameters(
    val swaps: Map<String, String> = emptyMap(),
    val constants: Map<String, Boolean> = emptyMap(),
    val dependencies: Map<String, Version> = emptyMap(),
    val replacements: ReplacementList = ReplacementList()
)