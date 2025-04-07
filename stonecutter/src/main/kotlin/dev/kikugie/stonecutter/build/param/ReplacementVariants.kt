package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.param.ReplacementVariantsImpl.replacementMap
import groovy.lang.Closure
import org.gradle.api.provider.Property
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.ApiStatus

public interface ReplacementVariants {
    @StonecutterAPI
    public fun replacement(
        direction: Boolean,
        from: String, to: String,
        phase: String = "LAST", id: Identifier? = null,
    )

    @StonecutterAPI
    public fun replacement(
        direction: Boolean,
        @Language("RegExp") fromPattern: String, toValue: String,
        @Language("RegExp") reversePattern: String, reverseValue: String,
        phase: String = "LAST", id: Identifier? = null,
    )

    @StonecutterAPI
    public infix fun replacement(properties: Map<String, Any>): Unit =
        replacementMap(properties)

    @StonecutterAPI
    public fun stringReplacement(build: StringReplacementBuilder.() -> Unit)

    @StonecutterAPI
    public fun stringReplacement(build: Closure<StringReplacementBuilder>): Unit =
        stringReplacement(build::call)

    @StonecutterAPI
    public fun regexReplacement(build: RegexReplacementBuilder.() -> Unit)

    @StonecutterAPI
    public fun regexReplacement(build: Closure<RegexReplacementBuilder>): Unit =
        regexReplacement(build::call)

    @StonecutterAPI
    public sealed interface ReplacementBuilder {
        public val direction: Property<Boolean>
        public val phase: Property<String>
        public val id: Property<Identifier>
    }

    @StonecutterAPI
    public interface StringReplacementBuilder : ReplacementBuilder {
        public val from: Property<String>
        public val to: Property<String>

        @ApiStatus.Internal
        public fun build(instance: ReplacementVariants): Unit = instance.replacement(
            direction.get(),
            from.get(), to.get(),
            phase.getOrElse("LAST"), id.orNull
        )
    }

    @StonecutterAPI
    public interface RegexReplacementBuilder : ReplacementBuilder {
        public val fromPattern: Property<String>
        public val toValue: Property<String>
        public val reversePattern: Property<String>
        public val reverseValue: Property<String>

        @ApiStatus.Internal
        public fun build(instance: ReplacementVariants): Unit = instance.replacement(
            direction.get(),
            fromPattern.get(), toValue.get(),
            reversePattern.get(), reverseValue.get(),
            phase.getOrElse("LAST"), id.orNull
        )
    }
}

private object ReplacementVariantsImpl {
    fun ReplacementVariants.replacementMap(properties: Map<String, Any>) = when {
        properties.containsAny("source", "from", "target", "to") -> stringReplacement(properties)
        properties.containsAny(
            "sourcePattern", "targetValue", "targetPattern", "sourceValue",
            "fromPattern", "toValue", "reversePattern", "reverseValue"
        ) -> regexReplacement(properties)
        else -> throw IllegalArgumentException("Invalid replacement properties: $properties")
    }

    private fun ReplacementVariants.stringReplacement(properties: Map<String, Any>) {
        val direction: Boolean = properties.getAs("direction")
        val source: String = properties.getAsOrNull("source")
            ?: properties.getAs("from")
        val target: String = properties.getAsOrNull("target")
            ?: properties.getAs("to")
        val phase: String = properties.getAsOrElse("phase", "LAST")
        val id: Identifier? = properties.getAsOrNull("id")
        replacement(direction, source, target, phase, id)
    }

    private fun ReplacementVariants.regexReplacement(properties: Map<String, Any>) {
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