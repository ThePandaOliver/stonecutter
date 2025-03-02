package dev.kikugie.stonecutter.build

import dev.kikugie.stitcher.data.Replacement
import dev.kikugie.stitcher.data.Replacement.Companion.regex
import dev.kikugie.stitcher.data.Replacement.Companion.string
import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.container.ConfigurationService.Companion.of
import dev.kikugie.stonecutter.data.parameters.BuildParameters
import dev.kikugie.stonecutter.controller.ControllerAbstraction
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

/**
 * Contains logic for the versioned buildscript, which is separated to allow
 * global configuration by [ControllerAbstraction.parameters].
 * @property hierarchy Path of the corresponding project
 */
public abstract class BuildAbstraction(protected val hierarchy: ProjectHierarchy, private val objects: ObjectFactory) :
    SwapVariants, ConstantVariants, DependencyVariants, FilterVariants, ReplacementVariants {
    protected val data: BuildParameters = checkNotNull(StonecutterPlugin.SERVICE.of(hierarchy).build) {
        "Stonecutter build parameters not found for $hierarchy. Present keys:\n%s"
            .format(StonecutterPlugin.SERVICE().parameters.buildParameters.keysToString())
    }

    override val swaps: MutableMap<Identifier, String> = CheckedMap(data.swaps) { k, _ -> k.validateId() }
    override val consts: MutableMap<Identifier, Boolean> = CheckedMap(data.constants) { k, _ -> k.validateId() }
    override val dependencies: DependencyVariants.VersionMap = CheckedMap(data.dependencies) { k, _ -> k.validateId() }
        .let(DependencyVariants::VersionMap)

    override val replacements: Collection<Replacement>
        get() = data.replacements

    override fun replacement(
        direction: Boolean,
        source: String,
        target: String,
        phase: String,
        identifier: Identifier?
    ) {
        require(source.isNotEmpty()) { "Source cannot be empty" }
        require(target.isNotEmpty()) { "Target cannot be empty" }
        if (identifier != null) require(identifier.isValid()) { "Invalid identifier: '$identifier'" }
        val realPhase = when (phase.lowercase()) {
            "first" -> Replacement.Phase.FIRST
            "last" -> Replacement.Phase.LAST
            else -> throw IllegalArgumentException("Invalid phase: '$phase', must be either 'FIRST' or 'LAST'")
        }
        if (direction) data.replacements.string(source, target, realPhase, identifier)
        else data.replacements.string(target, source, realPhase, identifier)
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
        require(sourcePattern.isNotEmpty()) { "Source pattern cannot be empty" }
        require(targetValue.isNotEmpty()) { "Target value cannot be empty" }
        require(targetPattern.isNotEmpty()) { "Target pattern cannot be empty" }
        require(sourceValue.isNotEmpty()) { "Source value cannot be empty" }
        if (identifier != null) require(identifier.isValid()) { "Invalid identifier: '$identifier'" }
        val realPhase = when (phase.lowercase()) {
            "first" -> Replacement.Phase.FIRST
            "last" -> Replacement.Phase.LAST
            else -> throw IllegalArgumentException("Invalid phase: '$phase', must be either 'FIRST' or 'LAST'")
        }
        if (direction) data.replacements.regex(sourcePattern, targetValue, realPhase, identifier)
        else data.replacements.regex(targetPattern, targetValue, realPhase, identifier)
    }

    override fun stringReplacement(action: Action<ReplacementVariants.StringReplacementBuilder>): Unit = objects
        .newInstance<ReplacementVariants.StringReplacementBuilder>()
        .also(action::execute).build(this)

    override fun regexReplacement(action: Action<ReplacementVariants.RegexReplacementBuilder>): Unit = objects
        .newInstance<ReplacementVariants.RegexReplacementBuilder>()
        .also(action::execute).build(this)

    override fun allowExtensions(extensions: Iterable<String>) {
        data.extensions += extensions
    }

    override fun overrideExtensions(extensions: Iterable<String>): Unit =
        data.extensions.clear() then allowExtensions(extensions)

    override fun excludeFiles(files: Iterable<String>): Unit = files.forEach {
        require(it.startsWith("src/")) { "File path must start with 'src/': $it" }
        Path(it).normalize().invariantSeparatorsPathString.let(data.exclusions::add)
    }

    internal fun from(other: BuildAbstraction): Unit = with(data) {
        swaps.putAll(other.data.swaps)
        constants.putAll(other.data.constants)
        dependencies.putAll(other.data.dependencies)
        extensions.addAll(other.data.extensions)
        exclusions.addAll(other.data.exclusions)
    }
}