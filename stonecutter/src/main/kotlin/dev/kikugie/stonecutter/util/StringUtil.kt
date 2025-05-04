package dev.kikugie.stonecutter.util

import dev.kikugie.stonecutter.StonecutterDevAPI

internal fun CharSequence.getOrDefault(index: Int, default: Char = ' ') =
    if (index >= 0 && index < length) get(index) else default

internal inline fun CharSequence.countIn(start: Int = 0, end: Int = length, predicate: (Char) -> Boolean): Int {
    var offset = start
    while (offset < end)
        if (predicate(this[offset])) offset++
        else break
    return offset - start
}

internal fun Char.isEnglishLetter() =
    this in 'a'..'z' || this in 'A'..'Z'

internal fun Char.isIdentifierStart() = when (this) {
    '_' -> true
    else -> isEnglishLetter()
}

internal fun Char.isIdentifierPart() = when (this) {
    '_', '-', '+', '.' -> true
    else -> isEnglishLetter() || isDigit()
}

/**
 * Checks if the string matches the [dev.kikugie.stonecutter.Identifier]
 * requirements.
 */
@StonecutterDevAPI
public fun isIdentifier(str: String): Boolean = str.isNotEmpty()
    && str.first().isIdentifierStart()
    && str.all(Char::isIdentifierPart)
