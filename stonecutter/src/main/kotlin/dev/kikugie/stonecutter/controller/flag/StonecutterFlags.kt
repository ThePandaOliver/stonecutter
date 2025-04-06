@file:JvmName("StonecutterFlags")
package dev.kikugie.stonecutter.controller.flag

public data class StonecutterFlag<T> (
    public val key: String,
    public val default: T
)

public val GENERATE_SOURCES_ON_SYNC: StonecutterFlag<Boolean> = StonecutterFlag("generateSourcesOnSync", true)
public val GENERATE_SWITCH_ACTIONS: StonecutterFlag<Boolean> = StonecutterFlag("generateSwitchActions", true)
public val IMPLICIT_RECEIVER: StonecutterFlag<String> = StonecutterFlag("implicitReceiver", "minecraft")
