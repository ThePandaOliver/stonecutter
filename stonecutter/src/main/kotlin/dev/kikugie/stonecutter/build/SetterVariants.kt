package dev.kikugie.stonecutter.build

import dev.kikugie.semver.Version
import dev.kikugie.stitcher.data.replacement.Replacement
import dev.kikugie.semver.SemanticVersion as SemanticVersionImpl
import dev.kikugie.stonecutter.*
import groovy.lang.Closure
import org.gradle.api.Action
import org.intellij.lang.annotations.Language
import kotlin.properties.Delegates

private inline fun <reified T> Map<String, Any>.getAs(key: String): T = requireAs<T>(this[key]) {
    if (it == null) "Missing property '$key'"
    else "Property '$key' is not of type ${T::class.simpleName}"
}

private fun Map<String, *>.containsAny(vararg keys: String): Boolean =
    keys.any { it in this }

internal class CheckedMap<K, V>(
    private val delegate: MutableMap<K, V>,
    private val check: (K, V) -> Unit,
) : MutableMap<K, V> by delegate {
    override fun put(key: K, value: V): V? = check(key, value) then delegate.put(key, value)
    override fun putAll(from: Map<out K, V>) = delegate.forEach { (k, v) -> check(k, v) } then delegate.putAll(from)
}

/**Declutters swap public function variants, directing them to a single implementation.*/
@SCDocumentation("swaps")
public interface SwapVariants {
    /**
     * Creates a swap with the given identifier and replacement value.
     * @sample stonecutter_samples.swaps.setter
     */
    @SCDocumentation("swaps.spec")
    @StonecutterAPI
    public val swaps: MutableMap<Identifier, String>

    /**
     * Creates a swap with the given [identifier] and [replacement] value.
     * @sample stonecutter_samples.swaps.single
     */
    @SCDocumentation("swaps.spec")
    @StonecutterAPI
    public fun swap(identifier: Identifier, replacement: String): Unit =
        swaps.set(identifier, replacement)

    /**
     * Creates a swap with the given [identifier] and [replacement] value.
     * @sample stonecutter_samples.swaps.provider
     */
    @SCDocumentation("swaps.spec")
    @StonecutterAPI
    public fun swap(identifier: Identifier, replacement: () -> String): Unit =
        swap(identifier, replacement())

    /**
     * Creates swaps with provided identifier - value pairs.
     * @sample stonecutter_samples.swaps.vararg
     */
    @SCDocumentation("swaps.spec")
    @StonecutterAPI
    public fun swaps(vararg values: Pair<Identifier, String>): Unit =
        swaps(values.asIterable())

    /**
     * Creates swaps with provided identifier - value pairs.
     * @sample stonecutter_samples.swaps.iterable
     */
    @SCDocumentation("swaps.spec")
    @StonecutterAPI
    public fun swaps(values: Iterable<Pair<Identifier, String>>): Unit =
        values.forEach { (id, str) -> swap(id, str) }

    /**
     * Creates swaps with provided identifier - value pairs.
     * @sample stonecutter_samples.swaps.map
     */
    @SCDocumentation("swaps.spec")
    @StonecutterAPI
    public fun swaps(values: Map<Identifier, String>): Unit =
        swaps(values.toList())
}

/**Declutters const public function variants, directing them to a single implementation.*/
@SCDocumentation("consts")
public interface ConstantVariants {
    /**
     * Creates a constant accessible in stonecutter conditions with the given identifier and corresponding boolean value.
     * @sample stonecutter_samples.constants.setter
     */
    @SCDocumentation("consts.spec")
    @StonecutterAPI
    public val consts: MutableMap<Identifier, Boolean>

    /**
     * Creates a constant accessible in stonecutter conditions with the given [identifier] and corresponding boolean [value].
     * @sample stonecutter_samples.constants.single
     */
    @SCDocumentation("consts.spec")
    @StonecutterAPI
    public fun const(identifier: Identifier, value: Boolean): Unit =
        consts.set(identifier, value)

    /**
     * Creates a constant accessible in stonecutter conditions with the given [identifier] and corresponding boolean [value].
     * @sample stonecutter_samples.constants.provider
     */
    @SCDocumentation("consts.spec")
    @StonecutterAPI
    public fun const(identifier: Identifier, value: () -> Boolean): Unit =
        const(identifier, value())

    /**
     * Creates constants with provided identifier - value pairs.
     * @sample stonecutter_samples.constants.vararg
     */
    @SCDocumentation("consts.spec")
    @StonecutterAPI
    public fun consts(vararg values: Pair<Identifier, Boolean>): Unit =
        consts(values.asIterable())

    /**
     * Creates constants with provided identifier - value pairs.
     * @sample stonecutter_samples.constants.iterable
     */
    @SCDocumentation("consts.spec")
    @StonecutterAPI
    public fun consts(values: Iterable<Pair<Identifier, Boolean>>): Unit =
        values.forEach { (id, str) -> const(id, str) }

    /**
     * Creates constants with provided identifier - value pairs.
     * @sample stonecutter_samples.constants.map
     */
    @SCDocumentation("consts.spec")
    @StonecutterAPI
    public fun consts(values: Map<Identifier, Boolean>): Unit =
        consts(values.toList())

    /**
     * Creates multiple constants from the [choices], checking whenever each is equal to the given [value].
     * @sample stonecutter_samples.constants.choices_vararg
     */
    @SCDocumentation("consts.choice")
    @StonecutterAPI
    public fun consts(value: Identifier, vararg choices: Identifier): Unit =
        consts(value, choices.asIterable())

    /**
     * Creates multiple constants from the [choices], checking whenever each is equal to the given [value].
     * @sample stonecutter_samples.constants.choices_iterable
     */
    @SCDocumentation("consts.choice")
    @StonecutterAPI
    public fun consts(value: Identifier, choices: Iterable<Identifier>): Unit =
        choices.forEach { const(it, it == value) }
}

/**Declutters dependency public function variants, directing them to a single implementation.*/
@SCDocumentation("deps")
public interface DependencyVariants {
    /**Mutable [Version] map that allows setting values from strings.*/
    public class VersionMap(delegate: MutableMap<Identifier, Version>) : MutableMap<Identifier, Version> by delegate {
        /**Converts the provided [value] to [SemanticVersionImpl] and stores it in the backing map.*/
        public operator fun set(key: Identifier, value: SemanticVersion): Unit = set(key, value.validateVersion())

        /**Converts the provided [value] to [SemanticVersionImpl] and stores it in the backing map.*/
        public fun put(key: Identifier, value: Identifier): Version? = put(key, value.validateVersion())

        /**Converts values in the provided [map] to [SemanticVersionImpl] and stores each pair in the backing map.*/
        public fun put(map: Map<out Identifier, Any>): Unit = map.forEach { (k, v) -> set(k, convert(v)) }

        private fun convert(value: Any): Version = when (value) {
            is String -> value.validateVersion()
            is Version -> value
            else -> throw IllegalArgumentException("Invalid version type: ${value::class.simpleName}")
        }
    }

    /**
     * Creates a dependency to the semver checks with the given identifier and corresponding version.
     * @sample stonecutter_samples.dependencies.setter
     */
    @SCDocumentation("deps.spec")
    @StonecutterAPI
    public val dependencies: VersionMap

    /**
     * Creates a dependency to the predicate checks with the given [identifier] and corresponding [version].
     * @sample stonecutter_samples.dependencies.single
     */
    @SCDocumentation("deps.spec")
    @StonecutterAPI
    public fun dependency(identifier: Identifier, version: SemanticVersion): Unit =
        dependencies.set(identifier, version)

    /**
     * Creates a dependency to the predicate checks with the given [identifier] and corresponding [version].
     * @sample stonecutter_samples.dependencies.provider
     */
    @SCDocumentation("deps.spec")
    @StonecutterAPI
    public fun dependency(identifier: Identifier, version: () -> SemanticVersion): Unit =
        dependency(identifier, version())

    /**
     * Creates dependencies to the predicate checks from provided identifier - version pairs.
     * @sample stonecutter_samples.dependencies.vararg
     */
    @SCDocumentation("deps.spec")
    @StonecutterAPI
    public fun dependencies(vararg values: Pair<Identifier, SemanticVersion>): Unit =
        dependencies(values.asIterable())

    /**
     * Creates dependencies to the predicate checks from provided identifier - version pairs.
     * @sample stonecutter_samples.dependencies.iterable
     */
    @SCDocumentation("deps.spec")
    @StonecutterAPI
    public fun dependencies(values: Iterable<Pair<Identifier, SemanticVersion>>): Unit =
        values.forEach { (id, ver) -> dependency(id, ver) }

    /**
     * Creates dependencies to the predicate checks from provided identifier - version pairs.
     * @sample stonecutter_samples.dependencies.map
     */
    @SCDocumentation("deps.spec")
    @StonecutterAPI
    public fun dependencies(values: Map<Identifier, SemanticVersion>): Unit =
        dependencies(values.toList())
}

public interface ReplacementVariants {
    /**
     * Accessor for the registered replacement data.
     * In Kotlin new entries can be added with the [plusAssign] operator.
     * @sample stonecutter_samples.replacements.dynamic_assign
     */
    public val replacements: Collection<Replacement>

    /**
     * Registers a string replacement, merging it with existing entries if possible.
     * @see dev.kikugie.stitcher.data.replacement.ReplacementList.addString
     * @sample stonecutter_samples.replacements.string_basic
     * @sample stonecutter_samples.replacements.string_ambiguous
     * @sample stonecutter_samples.replacements.string_circular
     * @sample stonecutter_samples.replacements.string_phased
     * @sample stonecutter_samples.replacements.string_identified
     */
    @StonecutterAPI
    public fun replacement(
        direction: Boolean,
        source: String,
        target: String,
        phase: String = "LAST",
        identifier: Identifier? = null,
    )

    /**
     * Registers a regex replacement
     * @see dev.kikugie.stitcher.data.replacement.ReplacementList.addRegex
     * @sample stonecutter_samples.replacements.regex_basic
     */
    @StonecutterAPI
    public fun replacement(
        direction: Boolean,
        @Language("RegExp") sourcePattern: String,
        targetValue: String,
        @Language("RegExp") targetPattern: String,
        sourceValue: String,
        phase: String = "LAST",
        identifier: Identifier? = null,
    )

    /**
     * Determines the replacement type from the given map parameters and registers it.
     * Key names and value types **must** match the ones from base methods, no value coercion is performed.
     * @sample stonecutter_samples.replacements.dynamic_assign
     * @sample stonecutter_samples.replacements.dynamic_map
     */
    @StonecutterAPI
    public fun replacement(properties: Map<String, Any>) {
        val direction: Boolean = properties.getAs("direction")
        val phase: String = properties.takeIf { "phase" in it }?.getAs("phase") ?: "LAST"
        val identifier: Identifier? = properties.takeIf { "identifier" in it }?.getAs("identifier")
        when {
            properties.containsAny("source", "target") ->
                replacement(direction, properties.getAs("source"), properties.getAs("target"), phase, identifier)

            properties.containsAny("sourcePattern", "targetValue", "targetPattern", "sourceValue") ->
                replacement(
                    direction, properties.getAs("sourcePattern"), properties.getAs("targetValue"),
                    properties.getAs("targetPattern"), properties.getAs("sourceValue"), phase, identifier
                )

            else ->
                throw IllegalArgumentException("Missing required replacement properties. Please check the documentation for the correct format.")
        }
    }

    /**
     * Registers a string replacement by configuring a [StringReplacementBuilder].
     * Can be used as a type-safe version of the map-based function.
     * @sample stonecutter_samples.replacements.string_configuration
     */
    @StonecutterAPI
    public fun stringReplacement(action: Action<StringReplacementBuilder>): Unit =
        StringReplacementBuilder().also(action::execute).build(this)

    /**
     * Registers a string replacement by configuring a [StringReplacementBuilder].
     * Can be used as a type-safe version of the map-based function.
     * @sample stonecutter_samples.replacements.string_configuration
     */
    @StonecutterAPI
    public fun stringReplacement(action: Closure<StringReplacementBuilder>): Unit =
        stringReplacement(action::call)

    /**
     * Registers a regex replacement by configuring a [RegexReplacementBuilder].
     * Can be used as a type-safe version of the map-based function.
     * @sample stonecutter_samples.replacements.regex_configuration
     */
    @StonecutterAPI
    public fun regexReplacement(action: Action<RegexReplacementBuilder>): Unit =
        RegexReplacementBuilder().also(action::execute).build(this)

    /**
     * Registers a regex replacement by configuring a [RegexReplacementBuilder].
     * Can be used as a type-safe version of the map-based function.
     * @sample stonecutter_samples.replacements.regex_configuration
     */
    @StonecutterAPI
    public fun regexReplacement(action: Closure<RegexReplacementBuilder>): Unit =
        regexReplacement(action::call)

    /**
     * Used with the [replacements] to add map-based replacements.
     * @sample stonecutter_samples.replacements.dynamic_assign
     */
    @StonecutterAPI
    public operator fun Collection<Replacement>.plusAssign(properties: Map<String, Any>): Unit = replacement(properties)

    /**@see ReplacementVariants.stringReplacement*/
    public class StringReplacementBuilder {
        public var direction: Boolean by Delegates.notNull<Boolean>()
        public lateinit var source: String
        public lateinit var target: String
        public lateinit var phase: String
        public lateinit var identifier: Identifier

        internal fun build(instance: ReplacementVariants) = instance
            .replacement(direction, source, target, phase, identifier)
    }

    /**@see ReplacementVariants.regexReplacement*/
    public class RegexReplacementBuilder {
        public var direction: Boolean by Delegates.notNull<Boolean>()
        @Language("RegExp")
        public lateinit var sourcePattern: String
        public lateinit var targetValue: String
        @Language("RegExp")
        public lateinit var targetPattern: String
        public lateinit var sourceValue: String
        public var phase: String = "LAST"
        public var identifier: Identifier? = null

        internal fun build(instance: ReplacementVariants) = instance
            .replacement(direction, sourcePattern, targetValue, targetPattern, sourceValue, phase, identifier)
    }
}

/**Declutters file filtering public function variants, directing them to a single implementation.*/
public interface FilterVariants {
    /**
     * Allows provided [extensions] to be processed by Stonecutter.
     * **Entries must not start with a dot**.
     *
     * @sample stonecutter_samples.allowExtensions.vararg
     * @see BuildParameters.extensions
     */
    @StonecutterAPI
    public fun allowExtensions(vararg extensions: String): Unit =
        allowExtensions(extensions.asIterable())

    /**
     * Allows provided [extensions] to be processed by Stonecutter.
     * **Entries must not start with a dot**.
     *
     * @sample stonecutter_samples.allowExtensions.iterable
     * @see BuildParameters.extensions
     */
    @StonecutterAPI
    public fun allowExtensions(extensions: Iterable<String>)

    /**
     * Replaces allowed extensions with the provided list.
     * **Entries must not start with a dot**.
     *
     * @sample stonecutter_samples.overrideExtensions.vararg
     */
    @StonecutterAPI
    public fun overrideExtensions(vararg extensions: String): Unit =
        overrideExtensions(extensions.asIterable())

    /**
     * Replaces allowed extensions with the provided list.
     * **Entries must not start with a dot**.
     *
     * @sample stonecutter_samples.overrideExtensions.iterable
     */
    @StonecutterAPI
    public fun overrideExtensions(extensions: Iterable<String>)

    /**
     * Excludes specific files or directories from being processed.
     * **Paths must be relative to the branch's directory and start with `src/`**.
     *
     * @sample stonecutter_samples.excludeFiles.vararg
     */
    @StonecutterAPI
    public fun excludeFiles(vararg files: String): Unit =
        excludeFiles(files.asIterable())

    /**
     * Excludes specific files or directories from being processed.
     * **Paths must be relative to the branch's directory and start with `src/`**.
     *
     * @sample stonecutter_samples.excludeFiles.iterable
     */
    @StonecutterAPI
    public fun excludeFiles(files: Iterable<String>)
}
