package dev.kikugie.stonecutter.build.dsl

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.SCConfiguration
import dev.kikugie.stonecutter.StonecutterAPI

@StonecutterAPI @SCConfiguration
public interface ConstantContainer : MutableMap<Identifier, Boolean> {
    public fun put(id: Identifier, value: () -> Boolean): Boolean? = put(id, value())

    public fun match(id: Identifier, vararg choices: Identifier) {
        for (it in choices) put(it, it == id)
    }

    public fun match(id: Identifier, choices: Iterable<Identifier>) {
        for (it in choices) put(it, it == id)
    }
}