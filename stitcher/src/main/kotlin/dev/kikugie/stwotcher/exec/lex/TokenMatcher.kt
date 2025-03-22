package dev.kikugie.stwotcher.exec.lex

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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import dev.kikugie.stwotcher.data.type.MarkerType.CONDITION as CONDITION_MARKER
import dev.kikugie.stwotcher.data.type.MarkerType.REPLACEMENT as REPLACEMENT_MARKER
import dev.kikugie.stwotcher.data.type.MarkerType.SWAP as SWAP_MARKER
import dev.kikugie.stwotcher.data.type.ReferenceType.REPLACEMENT as REPLACEMENT_REFERENCE
import dev.kikugie.stwotcher.data.type.ReferenceType.SWAP as SWAP_REFERENCE

@Suppress("DEPRECATION")
/**
 * Matches individual code tokens in the [source] sequence.
 * The matching process is stateless, unlike in the [CommentLexer].
 *
 * **Notes**:
 * - The [source] parameter should be a non-sliced string.
 *   The [TokenMatcher.match] method expects token boundaries,
 *   which will limit the string without additional copy operations.
 * - The default matcher created by [TokenMatcher.create] is a value class,
 *   so it can be created for each lexer advance call.
 */
sealed interface TokenMatcher {
    val source: CharSequence
    companion object {
        /**Empty token used as a marker in [InvalidMatcher.matchInvalid].*/
        private val EMPTY_SLICE = LexerSlice(IntRange.EMPTY, InvalidType, UNDEFINED)
        @OptIn(ExperimentalContracts::class)
        private inline fun bind(condition: Boolean, message: () -> String) {
            contract {
                returns() implies condition
                callsInPlace(message, InvocationKind.AT_MOST_ONCE)
            }
            if (!condition) throw IndexOutOfBoundsException(message())
        }

        fun create(source: CharSequence): TokenMatcher = StandardMatcher(source)
    }

    @JvmInline
    private value class StandardMatcher(override val source: CharSequence) : TokenMatcher

    @JvmInline
    private value class InvalidMatcher(override val source: CharSequence) : TokenMatcher {
        @Deprecated("Not a part of the public API")
        override fun matchInvalid(start: Int, offset: Int, end: Int, state: LexerState): LexerSlice = EMPTY_SLICE
    }

    /**
     * Matches a token segment [start]..<[end] in the [source] with the given lexer [state].
     * @return Matched token range and type, as well as the next state for the lexer.
     * @throws IndexOutOfBoundsException If `start < 0 || start >= end || end > source.length`
     */
    fun match(start: Int = 0, end: Int = source.length, state: LexerState = UNDEFINED): LexerSlice {
        bind(start >= 0) { "Start offset must be non-negative" }
        bind(end <= source.length) { "End index ($end) must not be greater than source length (${source.length})" }
        bind(start < end) { "Start index ($start) must be less than end index ($end)" }
        return matchImpl(start, end, state, source[start])
    }

    // All following methods expect start to be at least one character before end
    private fun matchImpl(start: Int, end: Int, state: LexerState, char: Char): LexerSlice = when (state) {
        UNDEFINED -> matchState(start, char)
        CONDITION -> matchCondition(start, end, char)
        SWAP -> matchSwap(start, end, char)
        REPLACEMENT -> matchReplacement(start, end, char)
        DONE -> error("DONE state expects no more characters")
    }

    private fun matchState(start: Int, char: Char): LexerSlice = when (char) {
        '?' -> LexerSlice(start extend 1, CONDITION_MARKER, CONDITION)
        '$' -> LexerSlice(start extend 1, SWAP_MARKER, SWAP)
        '~' -> LexerSlice(start extend 1, REPLACEMENT_MARKER, REPLACEMENT)
        else -> LexerSlice(start..<source.length, ScannedType.CONTENT, DONE)
    }

    private fun matchSwap(start: Int, end: Int, char: Char): LexerSlice = when (char) {
        '{' -> LexerSlice(start extend 1, SCOPE_OPEN, DONE)
        '}' -> LexerSlice(start extend 1, SCOPE_CLOSE, SWAP)
        '>' -> matchString(start, end, ">>", EXPECT_WORD, DONE)
            ?: matchInvalid(start, 1, end, SWAP)
        else -> when {
            char.isWhitespace() -> matchWhitespace(start, 1, end, SWAP)
            char.isIdentifierStart() -> matchIdentifier(start, 1, end, SWAP_REFERENCE, SWAP)
            else -> matchInvalid(start, 1, end, SWAP)
        }
    }

    private fun matchReplacement(start: Int, end: Int, char: Char): LexerSlice = when {
        char.isWhitespace() -> matchWhitespace(start, 1, end, REPLACEMENT)
        char.isIdentifierStart() -> matchIdentifier(start, 1, end, REPLACEMENT_REFERENCE, REPLACEMENT)
        else -> matchInvalid(start, 1, end, REPLACEMENT)
    }

    private fun matchCondition(start: Int, end: Int, char: Char): LexerSlice = when (char) {
        '<', '=', '~', '^' -> matchPredicate(start, end, char, PREDICATE)
        '{' -> LexerSlice(start extend 1, SCOPE_OPEN, DONE)
        '}' -> LexerSlice(start extend 1, SCOPE_CLOSE, CONDITION)
        '(' -> LexerSlice(start extend 1, GROUP_OPEN, CONDITION)
        ')' -> LexerSlice(start extend 1, GROUP_CLOSE, CONDITION)
        '!' -> LexerSlice(start extend 1, NEGATE, CONDITION)
        ':' -> LexerSlice(start extend 1, ASSIGN, CONDITION)
        '|' -> matchString(start, end, "||", OR, CONDITION)
            ?: matchInvalid(start, 1, end, CONDITION)

        '&' -> matchString(start, end, "&&", AND, CONDITION)
            ?: matchInvalid(start, 1, end, CONDITION)

        'i' -> matchString(start, end,"if", IF, CONDITION)
            ?: matchIdentifier(start, 1, end, UNRESOLVED, CONDITION)

        'e' -> matchString(start, end,"else", ELSE, CONDITION)
            ?: matchString(start, end,"elif", ELIF, CONDITION)
            ?: matchIdentifier(start, 1, end, UNRESOLVED, CONDITION)

        '>' -> matchString(start, end,">>", EXPECT_WORD, DONE)
            ?: matchPredicate(start, end, char, PREDICATE)

        else -> when {
            char.isWhitespace() -> matchWhitespace(start, 1, end, CONDITION)
            char.isIdentifierStart() -> matchIdentifier(start, 1, end, UNRESOLVED, CONDITION)
            else -> matchInvalid(start, 1, end, CONDITION)
        }
    }

    private fun matchString(start: Int, end: Int, pattern: String, type: TokenType, state: LexerState): LexerSlice? = when {
        start + pattern.length > end -> null
        source.substring(start, start + pattern.length) != pattern -> null
        else -> LexerSlice(start extend pattern.length, type, state)
    }

    private fun matchIdentifier(start: Int, offset: Int, end: Int, type: TokenType, state: LexerState): LexerSlice =
        matchSequence(start, offset, end, type, state, Char::isIdentifierPart)

    private fun matchWhitespace(start: Int, offset: Int, end: Int, state: LexerState): LexerSlice =
        matchSequence(start, offset, end, WhitespaceType, state, Char::isWhitespace)

    fun matchSequence(start: Int, offset: Int, end: Int, type: TokenType, state: LexerState, condition: (Char) -> Boolean): LexerSlice {
        var cursor = start + offset
        for (i in cursor..<end)
            if (condition(source[i])) cursor++
            else break
        return LexerSlice(start..<cursor, type, state)
    }

    private fun matchPredicate(start: Int, end: Int, char: Char, type: TokenType): LexerSlice {
        var cursor = start
        cursor += matchPredicateOperator(cursor, end, char)
        for (i in cursor..<end)
            if (source[i].isWhitespace()) cursor++
            else break
        if (cursor >= end) return LexerSlice(start..<end, InvalidType, DONE)

        val next = source[cursor]
        return if (!next.isDigit() && !(next.isIdentifierStart() && cursor != start)) matchInvalid(start, cursor + 1, end, CONDITION)
        else matchSequence(start, cursor + 1, end, type, CONDITION, Char::isIdentifierPart)
    }

    private fun matchPredicateOperator(start: Int, end: Int, char: Char): Int = when (char) {
        '=', '~', '^' -> 1
        '<', '>' -> if (end - start <= 1 || source.getOrDefault(start + 1) != '=') 1 else 2
        else -> 0
    }

    @Deprecated("Not a part of the public API")
    fun matchInvalid(start: Int, offset: Int, end: Int, state: LexerState): LexerSlice {
        var cursor = start + offset
        while (cursor < source.length)
            if (!source[cursor].isDefinitelyInvalid() && InvalidMatcher(source).match(cursor, end, state) !== EMPTY_SLICE) break
            else cursor++
        return LexerSlice(start..<cursor, InvalidType, state)
    }

    // Quick check before InvalidMatcher pass, since it's very inefficient.
    private fun Char.isDefinitelyInvalid() = when(this) {
        ',', '\'', '"', '*', '\\', '/', '=', ';', '#', '@', '[', ']', '%' -> true
        else -> false
    }
}