package dev.kikugie.stonecutter.controller.flag

public interface FlagContainer {
    public operator fun <T : Any> get(key: StonecutterFlag<T>): T
}

public interface MutableFlagContainer : FlagContainer {
    public operator fun <T : Any> set(key: StonecutterFlag<T>, value: T)
}