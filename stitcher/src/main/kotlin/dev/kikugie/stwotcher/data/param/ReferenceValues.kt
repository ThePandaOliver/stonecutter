package dev.kikugie.stwotcher.data.param

import dev.kikugie.semver.Version
import dev.kikugie.stitcher.data.replacement.ReplacementList

interface ReferenceValues {
    val swaps: Map<String, String>
    val constants: Map<String, Boolean>
    val dependencies: Map<String, Version>
    val replacements: ReplacementList
}