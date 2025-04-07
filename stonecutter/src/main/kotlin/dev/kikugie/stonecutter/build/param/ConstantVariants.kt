package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI

public interface ConstantVariants {
    /**Checked map for accessing and modifying constants used in file processing.*/
    @StonecutterAPI public val consts: MutableMap<Identifier, Boolean>

    /**Assigns the given constant [value] to the [id] in the [consts] map.*/
    @StonecutterAPI @Throws(IllegalArgumentException::class)
    public fun const(id: Identifier, value: Boolean) {
        consts[id] = value
    }

    /**Assigns the given constant [value] to the [id] in the [consts] map.*/
    @StonecutterAPI
    public fun const(id: Identifier, value: () -> Boolean) {
        consts[id] = value()
    }

    /**Adds the given constant [values] pairs to the [consts] map.*/
    @StonecutterAPI
    public fun consts(vararg values: Pair<Identifier, Boolean>) {
        consts.putAll(values)
    }

    /**Adds the given constant [values] pairs to the [consts] map.*/
    @StonecutterAPI
    public fun consts(values: Iterable<Pair<Identifier, Boolean>>) {
        consts.putAll(values)
    }

    /**Adds the given constant [values] pairs to the [consts] map.*/
    @StonecutterAPI
    public fun consts(values: Map<Identifier, Boolean>) {
        consts.putAll(values)
    }

    /**
     * Adds all values from [choices] to the [consts] map,
     * with each value being determined by comparing it to the [reference].
     */
    @StonecutterAPI
    public fun consts(reference: Identifier, vararg choices: Identifier) {
        for (it in choices) consts[it] = it == reference
    }

    /**
     * Adds all values from [choices] to the [consts] map,
     * with each value being determined by comparing it to the [reference].
     */
    @StonecutterAPI
    public fun consts(reference: Identifier, choices: Iterable<Identifier>) {
        for (it in choices) consts[it] = it == reference
    }
}