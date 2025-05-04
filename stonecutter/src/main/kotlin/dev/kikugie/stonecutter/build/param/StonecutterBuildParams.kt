package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.data.dsl.*
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import org.gradle.api.Action

@StonecutterAPI
public interface StonecutterBuildParams : VersionOperations<ParsedVersion> {
    public val constants: ConstantContainer
    public val dependencies: DependencyContainer
    public val swaps: SwapContainer
    public val replacements: ReplacementContainer
    public val filters: FilterContainer
    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations

    public fun constants(block: Action<ConstantContainer>): Unit = block.execute(constants)
    public fun dependencies(block: Action<DependencyContainer>): Unit = block.execute(dependencies)
    public fun swaps(block: Action<SwapContainer>): Unit = block.execute(swaps)
    public fun replacements(block: Action<ReplacementContainer>): Unit = block.execute(replacements)
    public fun filters(block: Action<FilterContainer>): Unit = block.execute(filters)
}

@Suppress("DEPRECATION")
public interface DeprecatedBuildParams : StonecutterBuildParams {
    @Deprecated("Use DSL block instead")
    public fun swap(id: Identifier, replacement: String) {
        swaps.put(id, replacement)
    }

    @Deprecated("Use DSL block instead")
    public fun swap(id: Identifier, replacement: () -> String) {
        swap(id, replacement())
    }

    @Deprecated("Use DSL block instead")
    public fun swaps(vararg values: Pair<Identifier, String>) {
        for ((id, replacement) in values) swap(id, replacement)
    }

    @Deprecated("Use DSL block instead")
    public fun swaps(values: Iterable<Pair<Identifier, String>>) {
        for ((id, replacement) in values) swap(id, replacement)
    }

    @Deprecated("Use DSL block instead")
    public fun swaps(values: Map<Identifier, String>) {
        for ((id, replacement) in values) swap(id, replacement)
    }

    @Deprecated("Use DSL block instead")
    public fun const(id: Identifier, value: Boolean) {
        constants.put(id, value)
    }

    @Deprecated("Use DSL block instead")
    public fun const(id: Identifier, value: () -> Boolean) {
        const(id, value())
    }

    @Deprecated("Use DSL block instead")
    public fun consts(vararg values: Pair<Identifier, Boolean>) {
        for ((id, value) in values) const(id, value)
    }

    @Deprecated("Use DSL block instead")
    public fun consts(values: Iterable<Pair<Identifier, Boolean>>) {
        for ((id, value) in values) const(id, value)
    }

    @Deprecated("Use DSL block instead")
    public fun consts(values: Map<Identifier, Boolean>) {
        for ((id, value) in values) const(id, value)
    }

    @Deprecated("Use DSL block instead")
    public fun consts(reference: Identifier, vararg choices: Identifier) {
        for (it in choices) const(it, it == reference)
    }

    @Deprecated("Use DSL block instead")
    public fun consts(reference: Identifier, choices: Iterable<Identifier>) {
        for (it in choices) const(it, it == reference)
    }

    @Deprecated("Use DSL block instead")
    public fun dependency(id: Identifier, version: Version) {
        dependencies.put(id, version)
    }

    @Deprecated("Use DSL block instead")
    public fun dependency(id: Identifier, version: () -> Version) {
        dependency(id, version())
    }

    @Deprecated("Use DSL block instead")
    public fun dependencies(vararg values: Pair<Identifier, Version>) {
        for ((id, version) in values) dependency(id, version)
    }

    @Deprecated("Use DSL block instead")
    public fun dependencies(values: Iterable<Pair<Identifier, Version>>) {
        for ((id, version) in values) dependency(id, version)
    }

    @Deprecated("Use DSL block instead")
    public fun dependencies(values: Map<Identifier, Version>) {
        for ((id, version) in values) dependency(id, version)
    }

    @Deprecated("Use DSL block instead")
    public fun allowExtensions(extensions: Iterable<String>) {
        filters.extensions.allow(extensions)
    }

    @Deprecated("Use DSL block instead")
    public fun allowExtensions(vararg extensions: String): Unit =
        allowExtensions(extensions.asIterable())

    @Deprecated("Use DSL block instead")
    public fun overrideExtensions(extensions: Iterable<String>) {
        filters.extensions.replace(extensions)
    }

    @Deprecated("Use DSL block instead")
    public fun overrideExtensions(vararg extensions: String): Unit =
        overrideExtensions(extensions.asIterable())

    @Deprecated("Use DSL block instead")
    public fun excludeFiles(files: Iterable<String>) {
        filters.excludes.exclude(files)
    }

    @Deprecated("Use DSL block instead")
    public fun excludeFiles(vararg files: String): Unit =
        excludeFiles(files.asIterable())

    @Deprecated("Use DSL block instead")
    public fun replacement(
        direction: Boolean,
        from: String, to: String,
        phase: String = "LAST", id: Identifier? = null,
    ): Unit = stringReplacement {
        this.direction.set(direction)
        this.from.set(from)
        this.to.set(to)
        this.phase.set(phase)
        if (id != null) this.id.set(id)
    }

    @Deprecated("Use DSL block instead")
    public fun replacement(
        direction: Boolean,
        fromPattern: String, toValue: String,
        reversePattern: String, reverseValue: String,
        phase: String = "LAST", id: Identifier? = null,
    ): Unit = regexReplacement {
        this.direction.set(direction)
        this.fromPattern.set(fromPattern)
        this.toValue.set(toValue)
        this.reversePattern.set(reversePattern)
        this.reverseValue.set(reverseValue)
        if (id != null) this.id.set(id)
    }

    @Deprecated("Use DSL block instead")
    public infix fun replacement(properties: Map<String, Any>): Unit =
        replacementMap(properties)

    @Deprecated("Use DSL block instead")
    public fun stringReplacement(build: Action<ReplacementContainer.StringReplacementBuilder>) {
        replacements.string(build)
    }

    @Deprecated("Use DSL block instead")
    public fun regexReplacement(build: Action<ReplacementContainer.RegexReplacementBuilder>) {
        replacements.regex(build)
    }

    private fun replacementMap(properties: Map<String, Any>) = when {
        properties.containsAny("source", "from", "target", "to") -> stringReplacement(properties)
        properties.containsAny(
            "sourcePattern", "targetValue", "targetPattern", "sourceValue",
            "fromPattern", "toValue", "reversePattern", "reverseValue"
        ) -> regexReplacement(properties)

        else -> throw IllegalArgumentException("Invalid replacement properties: $properties")
    }

    private fun stringReplacement(properties: Map<String, Any>) {
        val direction: Boolean = properties.getAs("direction")
        val source: String = properties.getAsOrNull("source")
            ?: properties.getAs("from")
        val target: String = properties.getAsOrNull("target")
            ?: properties.getAs("to")
        val phase: String = properties.getAsOrElse("phase", "LAST")
        val id: Identifier? = properties.getAsOrNull("id")
        replacement(direction, source, target, phase, id)
    }

    private fun regexReplacement(properties: Map<String, Any>) {
        val direction: Boolean = properties.getAs("direction")
        val fromPattern: String = properties.getAsOrNull("sourcePattern")
            ?: properties.getAs("fromPattern")
        val toValue: String = properties.getAsOrNull("targetValue")
            ?: properties.getAs("toValue")
        val reversePattern: String = properties.getAsOrNull("targetPattern")
            ?: properties.getAs("reversePattern")
        val reverseValue: String = properties.getAsOrNull("sourceValue")
            ?: properties.getAs("reverseValue")
        val phase: String = properties.getAsOrElse("phase", "LAST")
        val id: Identifier? = properties.getAsOrNull("id")
        replacement(direction, fromPattern, toValue, reversePattern, reverseValue, phase, id)
    }

    private inline fun <reified T : Any> Map<String, Any>.getAsOrElse(key: String, default: T) =
        getAsOrNull(key) ?: default

    private inline fun <reified T : Any> Map<String, Any>.getAs(key: String): T =
        requireNotNull(getAsOrNull(key)) { "Missing property '$key'" }

    private inline fun <reified T : Any> Map<String, Any>.getAsOrNull(key: String): T? = this[key]?.let {
        requireNotNull(it as? T) { "Property '$key' is not of type ${T::class.qualifiedName}" }
    }

    private fun Map<String, *>.containsAny(vararg keys: String): Boolean =
        keys.any { it in this }
}
