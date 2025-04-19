package dev.kikugie.semver.parsing

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.parsing.SemanticToken.Type
import dev.kikugie.semver.util.countIn
import dev.kikugie.semver.util.isEnglishLetter

internal fun parseImpl(input: CharSequence): Result<SemanticVersion> =
    SemanticParser(input).parse()

private data class SemanticToken(val type: Type, val value: String, val index: Int) {
    enum class Type { LITERAL, NUMERIC, DOT, DASH, PLUS, INVALID }
    val range: IntRange get() = index until (index + value.length)
}

private class SemanticLexer(val input: CharSequence) : Iterator<SemanticToken> {
    private var index = 0

    override fun hasNext(): Boolean = index < input.length
    override fun next(): SemanticToken = nextAny()
        .also { index += it.value.length }

    @Suppress("ComplexRedundantLet")
    private fun nextAny(): SemanticToken = when(val char = input[index]) {
        '.' -> SemanticToken(Type.DOT, char.toString(), index)
        '-' -> SemanticToken(Type.DASH, char.toString(), index)
        '+' -> SemanticToken(Type.PLUS, char.toString(), index)
        in '0'..'9' -> input.countIn(index, input.length, Char::isDigit)
            .let { SemanticToken(Type.NUMERIC, input.substring(index, index + it), index) }
        in 'a'..'z', in 'A'..'Z' -> input.countIn(index, input.length, Char::isEnglishLetter)
            .let { SemanticToken(Type.LITERAL, input.substring(index, index + it), index) }
        else -> SemanticToken(Type.INVALID, char.toString(), index)
    }
}

private class SemanticParser(val input: CharSequence) {
    fun parse(): Result<SemanticVersion> = runCatching {
        val tokens: ArrayDeque<SemanticToken> = Iterable { SemanticLexer(input) }
            .toList().let(::ArrayDeque)
        val components = parseHead(tokens)
        val preRelease = parsePreRelease(tokens)
        val buildData = parseBuildMetadata(tokens)
        SemanticVersion(components.toIntArray(), preRelease, buildData)
    }

    private fun parseHead(tokens: ArrayDeque<SemanticToken>): List<Int> = buildList {
        var hadNumber = false
        for (it in tokens.consuming()) when (it.type) {
            Type.NUMERIC ->
                it.value.toIntOrNull()?.let { this += it; hadNumber = true }
                    ?: throw VersionParsingException("Not a number", it.range)
            Type.DOT ->
                if (!hadNumber) throw VersionParsingException("Dot separator must follow a digit", it.range)
            Type.DASH, Type.PLUS ->
                if (!hadNumber) throw VersionParsingException("Metadata separator must follow a digit", it.range)
                else { tokens.addFirst(it); break }
            Type.LITERAL -> throw VersionParsingException("Letters are not allowed in version components", it.range)
            Type.INVALID -> throw VersionParsingException("Invalid character ${it.value}", it.range)
        }

        if (isEmpty()) throw VersionParsingException("Version components must not be empty", 0..0)
    }

    private fun parsePreRelease(tokens: ArrayDeque<SemanticToken>): String = buildString {
        if (tokens.firstOrNull()?.type != Type.DASH) return@buildString

        val start = tokens.firstOrNull()?.index ?: 0
        for (it in tokens.consuming()) when (it.type) {
            Type.NUMERIC, Type.LITERAL, Type.DASH, Type.DOT -> append(it.value)
            Type.PLUS ->
                if (isNotEmpty() && last().isLetterOrDigit()) { tokens.addFirst(it); break }
                else throw VersionParsingException("To start a build modifier, the pre-release must not be blank", it.range)
            Type.INVALID -> throw VersionParsingException("Invalid character ${it.value}", it.range)
        }

        if (isEmpty()) throw VersionParsingException("Pre-release must not be empty", start..start)
    }

    private fun parseBuildMetadata(tokens: ArrayDeque<SemanticToken>): String = buildString {
        if (tokens.firstOrNull()?.type != Type.PLUS) return@buildString

        val start = tokens.firstOrNull()?.index ?: 0
        for (it in tokens.consuming()) when (it.type) {
            Type.NUMERIC, Type.LITERAL, Type.DASH, Type.DOT -> append(it.value)
            Type.PLUS, Type.INVALID -> throw VersionParsingException("Invalid character ${it.value}", it.range)
        }

        if (isNotEmpty()) throw VersionParsingException("Build metadata must be empty", start..start)
    }

    private fun <T> ArrayDeque<T>.consuming(): Iterator<T> = object : Iterator<T> {
        override fun hasNext(): Boolean = isNotEmpty()
        override fun next(): T = removeFirst()
    }
}