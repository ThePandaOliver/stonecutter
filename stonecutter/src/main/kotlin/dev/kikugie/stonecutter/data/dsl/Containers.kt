package dev.kikugie.stonecutter.data.dsl

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.Version
import org.gradle.api.Action
import org.gradle.api.provider.Property

@StonecutterAPI @StonecutterParametersDSL
public interface ConstantContainer : DynamicMap<Identifier, Boolean> {
    /**
     * Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].
     */
    public fun match(sample: Identifier, vararg choices: Identifier): Unit = choices.forEach { put(it, it == sample) }
    /**
     * Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].
     */
    public fun match(sample: Identifier, choices: Iterable<Identifier>): Unit = choices.forEach { put(it, it == sample) }
}

@StonecutterAPI @StonecutterParametersDSL
public interface SwapContainer : DynamicMap<Identifier, String> {
}

@StonecutterAPI @StonecutterParametersDSL
public interface DependencyContainer : DynamicMap<Identifier, Version> {
}

@StonecutterAPI @StonecutterParametersDSL
public interface ReplacementContainer {
    @StonecutterReplacementSpec
    public interface StringReplacementBuilder {
        public val direction: Property<Boolean>
        public val phase: Property<String>
        public val id: Property<Identifier>
        public val from: Property<String>
        public val to: Property<String>

        public fun replace(from: String, to: String) {
            this.from.set(from)
            this.to.set(to)
        }
    }

    @StonecutterReplacementSpec
    public interface RegexReplacementBuilder {
        public val direction: Property<Boolean>
        public val phase: Property<String>
        public val id: Property<Identifier>
        public val fromPattern: Property<String>
        public val toValue: Property<String>
        public val reversePattern: Property<String>
        public val reverseValue: Property<String>

        public fun replace(from: String, to: String) {
            fromPattern.set(from)
            toValue.set(to)
        }

        public fun reverse(from: String, to: String) {
            reversePattern.set(from)
            reverseValue.set(to)
        }
    }

    public fun string(block: Action<StringReplacementBuilder>)
    public fun string(id: Identifier, block: Action<StringReplacementBuilder>): Unit =
        string { this.id.set(id); block.execute(this) }

    public fun regex(block: Action<RegexReplacementBuilder>)
    public fun regex(id: Identifier, block: Action<RegexReplacementBuilder>): Unit =
        regex { this.id.set(id); block.execute(this) }
}
