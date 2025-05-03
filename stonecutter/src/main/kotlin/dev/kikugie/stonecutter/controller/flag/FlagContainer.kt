package dev.kikugie.stonecutter.controller.flag

import dev.kikugie.stonecutter.StonecutterDevAPI

@StonecutterDevAPI
public sealed interface FlagContainer {
    public operator fun <T : Any> get(key: StonecutterFlag<T>): T
    public operator fun <T : Any> StonecutterFlag<T>.invoke(): T = get(this)
}

@StonecutterDevAPI
public sealed interface MutableFlagContainer : FlagContainer {
    public operator fun <T : Any> set(key: StonecutterFlag<T>, value: T)
    public operator fun <T : Any> StonecutterFlag<T>.invoke(value: T): Unit = set(this, value)
}

@Suppress("UNCHECKED_CAST")
internal class FlagContainerImpl() : MutableFlagContainer {
    constructor(values: Map<String, Any>): this() { flags.putAll(values) }
    val flags: MutableMap<String, Any> = mutableMapOf()

    override fun <T : Any> get(key: StonecutterFlag<T>): T =
        flags.getOrDefault(key.key, key.default) as T

    override fun <T : Any> set(key: StonecutterFlag<T>, value: T) =
        flags.set(key.key, value)
}