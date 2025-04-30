package dev.kikugie.stonecutter.data.dsl

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.Version

@StonecutterAPI @StonecutterParametersDSL
public interface ConstantContainer : MutableMap<Identifier, Boolean> {
    public fun put(id: Identifier, value: () -> Boolean): Boolean? = put(id, value())

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
public interface DependencyContainer : MutableMap<Identifier, Version> {
    public fun put(id: Identifier, version: () -> Version): Version? = put(id, version())
}

@StonecutterAPI @StonecutterParametersDSL
public interface SwapContainer : MutableMap<Identifier, String> {
    public fun put(id: Identifier, replacement: () -> String): String? = put(id, replacement())
}