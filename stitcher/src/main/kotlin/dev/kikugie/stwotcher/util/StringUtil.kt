package dev.kikugie.stwotcher.util

@JvmOverloads fun CharSequence.getOrDefault(index: Int, default: Char = ' ') =
    if (index >= 0 && index < length) get(index) else default

fun Char.isEnglishLetter() = this in 'a'..'z' || this in 'A'..'Z'

fun Char.isIdentifierStart() = when (this) {
    '_' -> true
    else -> isEnglishLetter()
}

fun Char.isIdentifierPart() = when (this) {
    '_', '-', '+', '.' -> true
    else -> isEnglishLetter() || isDigit()
}

fun String.isValidIdentifier(options: Set<String>): Boolean =
    if (isBlank() || !first().isIdentifierStart() || !all(Char::isIdentifierPart)) false
    else options.isEmpty() || options.contains(this)