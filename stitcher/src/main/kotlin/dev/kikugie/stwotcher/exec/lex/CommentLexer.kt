package dev.kikugie.stwotcher.exec.lex

import dev.kikugie.stwotcher.data.token.SourcedToken
import dev.kikugie.stwotcher.data.token.StitcherToken
import dev.kikugie.stwotcher.data.type.InvalidType
import dev.kikugie.stwotcher.data.token.slice
import dev.kikugie.stwotcher.util.LookaheadIterator

/**
 * Separates a given [source] token into a stream of stitcher code tokens.
 * Parameters [start] and [state] can be used to restore a serialized lexer to the given position.
 * Parameter [limit] is used to limit the processed string. This is used to detect unclosed comments,
 * which can be slow to lex due to the inefficiency of [TokenMatcher.matchInvalid].
 */
class CommentLexer(val source: StitcherToken, start: Int = 0, state: LexerState = LexerState.UNDEFINED, limit: Int = Int.MAX_VALUE) :
    LookaheadIterator<SourcedToken> {

    private val matcher: TokenMatcher
    private val strIndex: Int
    private val lexIndex: Int
    private var slice: LexerSlice? = null
    private val token: SourcedToken
        get() = slice?.token()
            ?: throw NoSuchElementException("Reached the end of the token stream")

    /**Current [start] and [state] parameters of the lexer, or `null` if it has reached the end of the [source].*/
    val position: Pair<Int, LexerState>?
        get() = slice?.let { it.range.last + 1 to it.state }

    init {
        require(limit > 0) { "Limit must be positive" }
        val full = source.value
        val lex = if (full.length > limit) full.take(limit) else full

        matcher = TokenMatcher.create(lex)
        strIndex = full.lastIndex
        lexIndex = lex.lastIndex
        restore(start, state)
    }

    override fun hasNext(): Boolean = slice != null
    override fun peek(): SourcedToken? = if (hasNext()) token else null
    override fun next(): SourcedToken = token.also { slice = slice!!.next() }

    /**Restores the lexer position to the given [start] and [state].*/
    fun restore(start: Int, state: LexerState) {
        require(start >= 0) { "Start index must be non-negative" }
        slice = when {
            start <= lexIndex -> matcher.match(start, state)
            start <= strIndex -> toInfinityAndBeyond(start)
            else -> null
        }
    }

    private fun LexerSlice.token(): SourcedToken = source.slice(range, type)

    private fun LexerSlice.next(): LexerSlice? = when {
        range.last < lexIndex -> matcher.match(range.last + 1, state)
        range.last < strIndex -> toInfinityAndBeyond(range.last + 1)
        else -> null
    }

    private fun toInfinityAndBeyond(start: Int) = LexerSlice(start..strIndex, InvalidType, LexerState.DONE)
}