package dev.kikugie.stonecutter.controller.flag

@Suppress("UNCHECKED_CAST")
internal class FlagContainerImpl : MutableFlagContainer {
    private val flags: MutableMap<String, Any> = mutableMapOf()

    override fun <T : Any> get(key: StonecutterFlag<T>): T =
        flags.getOrDefault(key.key, key.default) as T

    override fun <T : Any> set(key: StonecutterFlag<T>, value: T) =
        flags.set(key.key, value)
}