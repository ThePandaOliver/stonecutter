package dev.kikugie.stonecutter.build.dsl

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.SCConfiguration
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.Version

@StonecutterAPI @SCConfiguration
public interface DependencyContainer : MutableMap<Identifier, Version> {
    public fun put(id: Identifier, version: () -> Version): Version? = put(id, version())
}