package dev.kikugie.stwotcher.exec.lex

import dev.kikugie.stitcher.util.StringUtil.countStart
import dev.kikugie.stwotcher.data.type.InvalidType
import dev.kikugie.stwotcher.data.type.OperatorType.*
import dev.kikugie.stwotcher.data.type.SugarType.*
import dev.kikugie.stwotcher.data.type.ReferenceType.UNRESOLVED
import dev.kikugie.stwotcher.data.type.ReferenceType.PREDICATE
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.data.type.ScopeType.*
import dev.kikugie.stwotcher.data.type.TokenType
import dev.kikugie.stwotcher.data.type.WhitespaceType
import dev.kikugie.stwotcher.exec.lex.LexerState.*
import dev.kikugie.stwotcher.util.extend
import dev.kikugie.stwotcher.util.getOrDefault
import dev.kikugie.stwotcher.util.isIdentifierPart
import dev.kikugie.stwotcher.util.isIdentifierStart
import dev.kikugie.stwotcher.data.type.MarkerType.CONDITION as CONDITION_MARKER
import dev.kikugie.stwotcher.data.type.MarkerType.REPLACEMENT as REPLACEMENT_MARKER
import dev.kikugie.stwotcher.data.type.MarkerType.SWAP as SWAP_MARKER
import dev.kikugie.stwotcher.data.type.ReferenceType.REPLACEMENT as REPLACEMENT_REFERENCE
import dev.kikugie.stwotcher.data.type.ReferenceType.SWAP as SWAP_REFERENCE

@Suppress("DEPRECATION")
sealed interface TokenMatcher {
    val source: CharSequence
    companion object {
        /**Empty token used as a marker in [InvalidMatcher.matchInvalid].*/
        private val EMPTY_SLICE = LexerSlice(IntRange.EMPTY, InvalidType, UNDEFINED)
        @JvmStatic fun create(source: CharSequence): TokenMatcher = StandardMatcher(source)
    }

    @JvmInline
    private value class StandardMatcher(override val source: CharSequence) : TokenMatcher

    @JvmInline
    private value class InvalidMatcher(override val source: CharSequence) : TokenMatcher {
        @Deprecated("Not a part of the public API")
        override fun matchInvalid(offset: Int, state: LexerState): LexerSlice = EMPTY_SLICE
    }
    fun match(offset: Int = 0, state: LexerState = UNDEFINED): LexerSlice {
        require(offset >= 0) { "Offset must be non-negative" }
        require(offset < source.length) { "Offset $offset must be less than source length (${source.length})" }
        return matchImpl(offset, state, source[offset])
    }

    private fun matchImpl(offset: Int, state: LexerState, char: Char): LexerSlice = when (state) {
        UNDEFINED -> matchState(offset, char)
        CONDITION -> matchCondition(offset, char)
        SWAP -> matchSwap(offset, char)
        REPLACEMENT -> matchReplacement(offset, char)
        DONE -> error("DONE state expects no more characters")
    }

    private fun matchState(offset: Int, char: Char): LexerSlice = when (char) {
        '?' -> LexerSlice(offset extend 1, CONDITION_MARKER, CONDITION)
        '$' -> LexerSlice(offset extend 1, SWAP_MARKER, SWAP)
        '~' -> LexerSlice(offset extend 1, REPLACEMENT_MARKER, REPLACEMENT)
        else -> LexerSlice(offset until source.length, ScannedType.CONTENT, DONE)
    }

    private fun matchSwap(offset: Int, char: Char): LexerSlice = when (char) {
        '{' -> LexerSlice(offset extend 1, SCOPE_OPEN, DONE)
        '}' -> LexerSlice(offset extend 1, SCOPE_CLOSE, SWAP)
        '>' -> matchString(offset, ">>", EXPECT_WORD, DONE)
            ?: matchInvalid(offset, SWAP)
        else -> when {
            char.isWhitespace() -> matchWhitespace(offset, SWAP)
            char.isIdentifierStart() -> matchIdentifier(offset, SWAP_REFERENCE, SWAP)
            else -> matchInvalid(offset, SWAP)
        }
    }

    private fun matchReplacement(offset: Int, char: Char): LexerSlice = when {
        char.isWhitespace() -> matchWhitespace(offset, REPLACEMENT)
        char.isIdentifierStart() -> matchIdentifier(offset, REPLACEMENT_REFERENCE, REPLACEMENT)
        else -> matchInvalid(offset, REPLACEMENT)
    }

    private fun matchCondition(offset: Int, char: Char): LexerSlice = when (char) {
        '<', '=', '~', '^' -> matchPredicate(offset, char, PREDICATE)
        '{' -> LexerSlice(offset extend 1, SCOPE_OPEN, DONE)
        '}' -> LexerSlice(offset extend 1, SCOPE_CLOSE, CONDITION)
        '(' -> LexerSlice(offset extend 1, GROUP_OPEN, CONDITION)
        ')' -> LexerSlice(offset extend 1, GROUP_CLOSE, CONDITION)
        '!' -> LexerSlice(offset extend 1, NEGATE, CONDITION)
        ':' -> LexerSlice(offset extend 1, ASSIGN, CONDITION)
        '|' -> matchString(offset, "||", OR, CONDITION)
            ?: matchInvalid(offset, CONDITION)

        '&' -> matchString(offset, "&&", AND, CONDITION)
            ?: matchInvalid(offset, CONDITION)

        'i' -> matchString(offset, "if", IF, CONDITION)
            ?: matchIdentifier(offset, UNRESOLVED, CONDITION)

        'e' -> matchString(offset, "else", ELSE, CONDITION)
            ?: matchString(offset, "elif", ELIF, CONDITION)
            ?: matchIdentifier(offset, UNRESOLVED, CONDITION)

        '>' -> matchString(offset, ">>", EXPECT_WORD, DONE)
            ?: matchPredicate(offset, char, PREDICATE)

        else -> when {
            char.isWhitespace() -> matchWhitespace(offset, CONDITION)
            char.isIdentifierStart() -> matchIdentifier(offset, UNRESOLVED, CONDITION)
            else -> matchInvalid(offset, CONDITION)
        }
    }

    private fun matchString(offset: Int, pattern: String, type: TokenType, state: LexerState): LexerSlice? = when {
        offset + pattern.length > source.length -> null
        source.substring(offset, offset + pattern.length) != pattern -> null
        else -> LexerSlice(offset extend pattern.length, type, state)
    }

    private fun matchIdentifier(offset: Int, type: TokenType, state: LexerState): LexerSlice = source
        .countStart(offset, Char::isIdentifierPart)
        .let { LexerSlice(offset extend it, type, state) }

    private fun matchWhitespace(offset: Int, state: LexerState): LexerSlice = source
        .countStart(offset, Char::isWhitespace)
        .let { LexerSlice(offset extend it, WhitespaceType, state) }

    private fun matchPredicate(offset: Int, char: Char, type: TokenType): LexerSlice {
        var cursor = offset
        cursor += matchPredicateOperator(cursor, char)
        cursor += source.countStart(offset, Char::isWhitespace)

        val next = source.getOrDefault(cursor)
        return when {
            next == ' ' -> LexerSlice(offset until cursor, InvalidType, DONE)
            next.isDigit() || next.isIdentifierStart() && cursor != offset -> source
                .countStart(++cursor, Char::isIdentifierPart)
                .let { LexerSlice(offset until cursor + it, type, CONDITION) }
            else -> matchInvalid(offset, CONDITION)
        }
    }

    private fun matchPredicateOperator(offset: Int, char: Char): Int = when (char) {
        '=', '~', '^' -> 1
        '<', '>' -> if (source.getOrDefault(offset + 1) == '=') 2 else 1
        else -> 0
    }

    @Deprecated("Not a part of the public API")
    fun matchInvalid(offset: Int, state: LexerState): LexerSlice {
        var cursor = offset
        while (++cursor < source.length) {
            if (source[cursor].isDefinitelyInvalid()) continue
            val match = InvalidMatcher(source).match(cursor, state)
            if (match.type != InvalidType) break
        }
        return LexerSlice(offset until cursor, InvalidType, state)
    }

    /**Quick check before [InvalidMatcher] pass, since it's very inefficient.*/
    private fun Char.isDefinitelyInvalid() = when(this) {
        ',', '\'', '"', '*', '\\', '/', '=', ';', '#', '@', '[', ']', '%' -> true
        else -> false
    }
}