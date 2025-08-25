package dev.kikugie.stonecutter.data.dsl

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.Version
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.provider.Property

@StonecutterAPI @StonecutterParametersDSL
public interface ConstantContainer : DynamicMap<Identifier, Boolean> {
    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, vararg choices: Identifier): Unit = choices.forEach { put(it, it == sample) }

    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, choices: Iterable<Identifier>): Unit = choices.forEach { put(it, it == sample) }
}

@StonecutterAPI @StonecutterParametersDSL
public interface SwapContainer : DynamicMap<Identifier, String>

@StonecutterAPI @StonecutterParametersDSL
public interface DependencyContainer : DynamicMap<Identifier, Version>

@StonecutterAPI @StonecutterParametersDSL
public interface ReplacementContainer {
    @StonecutterReplacementSpec
    public interface StringReplacementBuilder {
        public val direction: Property<Boolean>
        @get:Deprecated("To be removed in 0.8 due to the file processing engine rework.")
        public val phase: Property<String>
        public val id: Property<Identifier>
        public val from: Property<String>
        public val to: Property<String>

        public fun replace(from: String, to: String) {
            this.from.value(from).disallowChanges()
            this.to.value(to).disallowChanges()
        }
    }

    @StonecutterReplacementSpec
    public interface RegexReplacementBuilder {
        public val direction: Property<Boolean>
        @get:Deprecated("To be removed in 0.8 due to the file processing engine rework.")
        public val phase: Property<String>
        public val id: Property<Identifier>
        public val fromPattern: Property<String>
        public val toValue: Property<String>
        public val reversePattern: Property<String>
        public val reverseValue: Property<String>

        public fun replace(from: String, to: String) {
            fromPattern.value(from).disallowChanges()
            toValue.value(to).disallowChanges()
        }

        public fun reverse(from: String, to: String) {
            reversePattern.value(from).disallowChanges()
            reverseValue.value(to).disallowChanges()
        }
    }

    public fun string(action: Action<StringReplacementBuilder>)
    public fun string(action: Closure<*>): Unit = string(action::call)

    public fun string(direction: Boolean, action: Action<StringReplacementBuilder>): Unit =
        string { this.direction.set(direction); action.execute(this) }
    public fun string(direction: Boolean, action: Closure<*>): Unit =
        string(direction, action::call)

    public fun string(id: Identifier, direction: Boolean? = null, action: Action<StringReplacementBuilder>): Unit =
        string { this.id.set(id); this.direction.set(direction); action.execute(this) }
    public fun string(id: Identifier, direction: Boolean? = null, action: Closure<*>): Unit =
        string(id, direction, action::call)

    public fun regex(action: Action<RegexReplacementBuilder>)
    public fun regex(action: Closure<*>): Unit = regex(action::call)

    public fun regex(direction: Boolean, action: Action<RegexReplacementBuilder>): Unit =
        regex { this.direction.set(direction); action.execute(this) }
    public fun regex(direction: Boolean, action: Closure<*>): Unit =
        regex(direction, action::call)

    public fun regex(id: Identifier, direction: Boolean? = null, action: Action<RegexReplacementBuilder>): Unit =
        regex { this.id.set(id); this.direction.set(direction); action.execute(this) }
    public fun regex(id: Identifier, direction: Boolean? = null, action: Closure<*>): Unit =
        regex(id, direction, action::call)
}
