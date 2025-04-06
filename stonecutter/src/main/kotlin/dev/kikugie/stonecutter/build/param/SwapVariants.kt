package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.build.ParameterMap

public interface SwapVariants {
    @StonecutterAPI
    public val swaps: ParameterMap<Identifier, String>

    @StonecutterAPI
    public fun swap(id: Identifier, replacement: String) {
        swaps[id] = replacement
    }

    @StonecutterAPI
    public fun swap(id: Identifier, replacement: () -> String) {
        swaps[id] = replacement()
    }

    @StonecutterAPI
    public fun swaps(vararg values: Pair<Identifier, String>) {
        swaps.putAll(values)
    }

    @StonecutterAPI
    public fun swaps(values: Iterable<Pair<Identifier, String>>) {
        swaps.putAll(values)
    }

    @StonecutterAPI
    public fun swaps(values: Map<Identifier, String>) {
        swaps.putAll(values)
    }
}