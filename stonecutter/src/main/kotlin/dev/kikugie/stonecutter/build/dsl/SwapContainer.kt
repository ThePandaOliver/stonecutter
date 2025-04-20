package dev.kikugie.stonecutter.build.dsl

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.SCConfiguration
import dev.kikugie.stonecutter.StonecutterAPI

@StonecutterAPI @SCConfiguration
public interface SwapContainer : MutableMap<Identifier, String> {
    public fun put(id: Identifier, replacement: () -> String): String? = put(id, replacement())
}