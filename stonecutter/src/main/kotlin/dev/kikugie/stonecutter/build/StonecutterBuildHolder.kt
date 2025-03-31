package dev.kikugie.stonecutter.build

import dev.kikugie.stitcher.data.replacement.Replacement
import dev.kikugie.stitcher.data.replacement.ReplacementList
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.isValid

public class StonecutterBuildHolder : StonecutterBuildParams {
    override val swaps: MutableMap<Identifier, String> = mutableMapOf()
    override val consts: MutableMap<Identifier, Boolean> = mutableMapOf()
    override val dependencies: DependencyVariants.VersionMap = DependencyVariants.VersionMap(mutableMapOf())
    override val replacements: Collection<Replacement> get() = _replacements.delegate
    private val _replacements = ReplacementList()

    override fun replacement(
        direction: Boolean,
        source: String,
        target: String,
        phase: String,
        identifier: Identifier?
    ) {
        if (identifier != null) require(identifier.isValid()) { "Invalid identifier: '$identifier'" }
        val realPhase = when (phase.lowercase()) {
            "first" -> ReplacementPhase.FIRST
            "last" -> ReplacementPhase.LAST
            else -> throw IllegalArgumentException("Invalid phase: '$phase', must be either 'FIRST' or 'LAST'")
        }
        if (direction) _replacements.addString(source, target, realPhase, identifier)
        else _replacements.addString(target, source, realPhase, identifier)
    }

    override fun replacement(
        direction: Boolean,
        sourcePattern: String,
        targetValue: String,
        targetPattern: String,
        sourceValue: String,
        phase: String,
        identifier: Identifier?
    ) {
        if (identifier != null) require(identifier.isValid()) { "Invalid identifier: '$identifier'" }
        val realPhase = when (phase.lowercase()) {
            "first" -> ReplacementPhase.FIRST
            "last" -> ReplacementPhase.LAST
            else -> throw IllegalArgumentException("Invalid phase: '$phase', must be either 'FIRST' or 'LAST'")
        }
        val sourceRegex = sourcePattern.toRegex()
        if (direction) _replacements.addRegex(sourceRegex, targetValue, realPhase, identifier)
        else _replacements.addRegex(sourceRegex, targetValue, realPhase, identifier)
    }

    override fun allowExtensions(extensions: Iterable<String>) {
    }

    override fun overrideExtensions(extensions: Iterable<String>) {

    }

    override fun excludeFiles(files: Iterable<String>) {

    }

}