package dev.kikugie.stonecutter.data.dsl

import dev.kikugie.semver.VersionParser
import dev.kikugie.semver.Version as ParsedVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.StonecutterAPI
import org.jetbrains.annotations.ApiStatus

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
public interface DependencyContainer : MutableMap<Identifier, ParsedVersion> {
    public fun put(id: Identifier, version: () -> Version): ParsedVersion? = put(id, version())

    public operator fun set(id: Identifier, version: Version): ParsedVersion? = put(id, version)
    public fun put(id: Identifier, version: Version): ParsedVersion? = put(id, parse(version))
    public fun putIfAbsent(id: Identifier, value: Version): ParsedVersion? = if (id !in this) put(id, value) else null

    @ApiStatus.Experimental
    public fun parse(version: Version): ParsedVersion = VersionParser.parseLenient(version, full = true).value
}

@StonecutterAPI @StonecutterParametersDSL
public interface SwapContainer : MutableMap<Identifier, String> {
    public fun put(id: Identifier, replacement: () -> String): String? = put(id, replacement())
}