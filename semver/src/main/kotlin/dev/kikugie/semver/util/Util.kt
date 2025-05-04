package dev.kikugie.semver.util

internal fun CharSequence.getOrDefault(index: Int, default: Char = ' '): Char =
    if (index in indices) get(index) else default

internal fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
internal fun Char.allowedInStringVer(): Boolean =
    isDigit() || isEnglishLetter() || this == '_' || this == '-'

internal inline fun CharSequence.countIn(start: Int = 0, end: Int = length, predicate: (Char) -> Boolean): Int {
    var offset = start
    while (offset < end)
        if (predicate(this[offset])) offset++
        else break
    return offset - start
}