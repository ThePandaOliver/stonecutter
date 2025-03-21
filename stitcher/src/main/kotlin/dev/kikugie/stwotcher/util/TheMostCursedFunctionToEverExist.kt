package dev.kikugie.stwotcher.util

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> Any?.then(other: T): T = other

inline fun <reified T : Any> Any.takeAs(): T? = this as? T