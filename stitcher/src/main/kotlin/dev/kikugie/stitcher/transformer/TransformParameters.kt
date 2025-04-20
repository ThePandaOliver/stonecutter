package dev.kikugie.stitcher.transformer

import dev.kikugie.semver.LenientVersionOperations
import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.data.replacement.ReplacementList
import kotlinx.serialization.Serializable

@Serializable
data class TransformParameters(
    val swaps: Map<String, String> = emptyMap(),
    val constants: Map<String, Boolean> = emptyMap(),
    val dependencies: Map<String, Version> = emptyMap(),
    val replacements: ReplacementList = ReplacementList()
) {
    companion object {
        inline fun TransformParameters(build: TransformParametersBuilder.() -> Unit) =
            TransformParametersBuilder().apply(build).build()
    }

    class TransformParametersBuilder {
        val swaps: MutableMap<String, String> = mutableMapOf()
        val constants: MutableMap<String, Boolean> = mutableMapOf()
        val dependencies: MutableMap<String, String> = mutableMapOf()
        val replacements: ReplacementList = ReplacementList()

        fun build() = TransformParameters(
            swaps.toMap(),
            constants.toMap(),
            dependencies.mapValues { LenientVersionOperations.parseVersion(it.value).getOrThrow() },
            replacements
        )
    }
}