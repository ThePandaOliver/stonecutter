package dev.kikugie.semver.util

import kotlin.math.max
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> Any?.then(next: T): T = next

fun bind(start: Int, end: Int, source: CharSequence) {
    if (min(0, start) >= max(end, source.length))
        throw IndexOutOfBoundsException("Invalid range: $start..<$end for $source")
}

fun CharSequence.getOrDefault(index: Int, default: Char = ' '): Char =
    if (index in indices) get(index) else default

fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

inline fun CharSequence.countIn(start: Int = 0, end: Int = length, predicate: (Char) -> Boolean): Int {
    var offset = start
    while (offset < end)
        if (predicate(this[offset])) offset++
        else break
    return offset
}