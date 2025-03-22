package dev.kikugie.stwotcher.exec.lex

import dev.kikugie.stwotcher.data.token.SourcedToken
import dev.kikugie.stwotcher.data.token.StitcherToken
import dev.kikugie.stwotcher.data.type.InvalidType
import dev.kikugie.stwotcher.util.LookaheadIterable
import dev.kikugie.stwotcher.util.LookaheadIterator
import dev.kikugie.stwotcher.util.mutableLazy
import dev.kikugie.stwotcher.util.slice

/**
 * Separates a given [source] token into a stream of stitcher code tokens.
 * This lexers state can be retrieved with [CommentLexer.position] and restored with [CommentLexer.restore].
 * Restoring state on a lexer with different [host] and [limit] parameters has undefined behaviour.
 *
 * **Notes**:
 * - The token stream includes whitespace tokens, which usually need to be filtered with
 *   ```kotlin
 *      val token: StitcherToken = "? if my_constant {".toToken(ScannedType.CONTENT)
 *      val lexer: LookaheadIterator<StitcherToken> = CommentLexer.create(token)
 *      val filtered: LookaheadIterator<StitcherToken> = lexer.filter {
 *          !it.isOf<WhitespaceType>()
 *      }
 *   ```
 *   The whitespaces are preserved for reusability, such as for the IntelliJ plugin,
 *   which requires whitespace tokens to be retained.
 */
class CommentLexer(val host: StitcherToken, limit: Int = Int.MAX_VALUE) :
    LookaheadIterator<StitcherToken> {
    companion object {
        fun create(source: StitcherToken, limit: Int = Int.MAX_VALUE): LookaheadIterator<out StitcherToken> =
            CommentLexer(source, limit)
        fun iterable(source: StitcherToken, limit: Int = Int.MAX_VALUE): LookaheadIterable<out StitcherToken> = object : LookaheadIterable<StitcherToken> {
            override fun iterator(): LookaheadIterator<StitcherToken> = CommentLexer(source, limit)
        }
    }

    /**The current lexer position and state, or `null` if it has reached the end of the sequence.*/
    val position: Pair<Int, LexerState>?
        get() = slice?.let { it.range.last + 1 to it.state }

    private var slice: LexerSlice? by mutableLazy { createSlice(0, LexerState.UNDEFINED) }
    private val limit = limit.coerceAtMost(host.range.last - host.range.first + 1)
    private val source: CharSequence get() = host.source
    private val token: SourcedToken
        get() = slice?.token() ?: throw NoSuchElementException("Reached the end of the token stream")

    init {
        require(limit > 0) { "Limit must be positive" }
    }

    override fun hasNext(): Boolean = slice != null
    override fun peek(): SourcedToken? = if (hasNext()) token else null
    override fun next(): SourcedToken = token.also { slice = slice!!.next() }

    /**Restores the lexer position to the given [start] and [state].*/
    fun restore(start: Int, state: LexerState) {
        slice = createSlice(start, state)
    }

    private fun LexerSlice.token(): SourcedToken = host.slice(range, type)

    private fun LexerSlice.next() = createSlice(range.last + 1, state)

    private fun toInfinityAndBeyond(start: Int) = LexerSlice(start..<source.length, InvalidType, LexerState.DONE)

    private fun createSlice(start: Int, state: LexerState): LexerSlice? {
        require(start >= 0) { "Start index must be non-negative" }
        return when {
            state == LexerState.DONE -> null
            start < limit -> TokenMatcher.create(source).match(host.range.first + start, host.range.first + limit, state)
            start < source.length -> toInfinityAndBeyond(host.range.first + start)
            else -> null
        }
    }
}