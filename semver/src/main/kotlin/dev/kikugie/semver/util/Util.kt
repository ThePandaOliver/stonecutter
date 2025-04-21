package dev.kikugie.semver.util

import dev.kikugie.semver.parsing.VersionParsingException
import kotlin.math.max
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
internal inline infix fun <T> Any?.then(next: T): T = next

internal fun bind(start: Int, end: Int, source: CharSequence) {
    if (min(0, start) >= max(end, source.length))
        throw IndexOutOfBoundsException("Invalid range: $start..<$end for $source")
}

internal fun CharSequence.getOrDefault(index: Int, default: Char = ' '): Char =
    if (index in indices) get(index) else default

internal fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

internal inline fun CharSequence.countIn(start: Int = 0, end: Int = length, predicate: (Char) -> Boolean): Int {
    var offset = start
    while (offset < end)
        if (predicate(this[offset])) offset++
        else break
    return offset - start
}

fun <T> Result<T>.formatParsingException(source: CharSequence, offset: Int = 0): Result<T> = mapException {
    if (it is VersionParsingException) it.formatted(source, offset) else it
}

inline fun <T> Result<T>.mapException(mapping: (Throwable) -> Throwable): Result<T> =
    if (isFailure) Result.failure(mapping(exceptionOrNull()!!)) else this