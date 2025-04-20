package dev.kikugie.stonecutter.controller.flag

import dev.kikugie.stonecutter.SCFlagSpec

@SCFlagSpec
public interface FlagContainer {
    public operator fun <T : Any> get(key: StonecutterFlag<T>): T
    public operator fun <T : Any> StonecutterFlag<T>.invoke(): T = get(this)
}

public interface MutableFlagContainer : FlagContainer {
    public operator fun <T : Any> set(key: StonecutterFlag<T>, value: T)
    public operator fun <T : Any> StonecutterFlag<T>.invoke(value: T): Unit = set(this, value)
}