package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.container.ParameterMap

public interface ConstantVariants {
    @StonecutterAPI
    public val consts: ParameterMap<Identifier, Boolean>

    @StonecutterAPI
    public fun const(id: Identifier, value: Boolean) {
        consts[id] = value
    }

    @StonecutterAPI
    public fun const(id: Identifier, value: () -> Boolean) {
        consts[id] = value()
    }

    @StonecutterAPI
    public fun consts(vararg values: Pair<Identifier, Boolean>) {
        consts.putAll(values)
    }

    @StonecutterAPI
    public fun consts(values: Iterable<Pair<Identifier, Boolean>>) {
        consts.putAll(values)
    }

    @StonecutterAPI
    public fun consts(values: Map<Identifier, Boolean>) {
        consts.putAll(values)
    }

    @StonecutterAPI
    public fun consts(reference: Identifier, vararg choices: Identifier) {
        for (it in choices) consts[it] = it == reference
    }

    @StonecutterAPI
    public fun consts(reference: Identifier, choices: Iterable<Identifier>) {
        for (it in choices) consts[it] = it == reference
    }
}