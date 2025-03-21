package dev.kikugie.stwotcher.exec.scan

import dev.kikugie.stwotcher.data.token.SourcedToken
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.util.FixedQueue
import dev.kikugie.stwotcher.util.LookaheadIterable
import dev.kikugie.stwotcher.util.LookaheadIterator
import dev.kikugie.stwotcher.util.getOrDefault

class CommentScanner (
    val source: CharSequence,
    val recognizers: Iterable<CommentRecognizer>
) : LookaheadIterator<SourcedToken> {
    companion object {
        fun create(source: CharSequence, recognizers: Iterable<CommentRecognizer>) = CommentScanner(source, recognizers)
        fun iterable(source: CharSequence, recognizers: Iterable<CommentRecognizer>) : LookaheadIterable<SourcedToken> = object : LookaheadIterable<SourcedToken> {
            override fun iterator(): LookaheadIterator<SourcedToken> = CommentScanner(source, recognizers)
        }
    }

    /**
     * Length of the input.
     *
     * *Calling `source.length` every time makes it 9% slower, good job JVM constant evaluation!*
     */
    private val length = source.length

    /**
     * Current position in the input string.
     */
    private var cursor = 0

    /**
     * Last appended position. Used to remember where to slice the string.
     */
    private var checkpoint = 0

    /**
     * The current quote enclosure type. Comments are not detected if it's not null.
     * Updated by [quoteStatus].
     */
    private var quote: QuoteType? = null

    /**
     * The current comment type. Must match [CommentRecognizer.end] to exit the comment.
     */
    private var comment: CommentRecognizer? = null

    /**
     * Stores the generated tokens, depleting before [advance] can be called.
     */
    private var buffer: FixedQueue<SourcedToken> = FixedQueue(4)

    init {
        advance()
    }

    override fun hasNext(): Boolean = buffer.isNotEmpty()

    override fun next(): SourcedToken = when(buffer.size) {
        0 -> throw NoSuchElementException("Reached the end of the token stream")
        1 -> advance().let { buffer.remove() }
        else -> buffer.remove()
    }

    override fun peek(): SourcedToken? =
        buffer.lastOrNull()

    private fun advance(): Boolean {
        if (cursor >= length) return false
        while (true) {
            if (!advanceQuote())
                return wrapRemaining()
            val added = advanceRecognizers()
            if (cursor >= length)
                return wrapRemaining()
            if (added) return true
            cursor++
        }
    }

    private fun wrapRemaining(): Boolean {
        if (cursor > checkpoint)
            if (comment != null) addPlaceholders()
            else token(checkpoint, length, ScannedType.CONTENT)
        return true
    }

    private fun addPlaceholders() {
        token(checkpoint, length, ScannedType.COMMENT_BODY)
        token(length, length, ScannedType.COMMENT_END)
    }

    private fun advanceQuote(): Boolean { // True when can process further
        while (quoteStatus()) if (++cursor >= length) return false
        return true
    }

    private fun advanceRecognizers(): Boolean { // True when have added to the list
        if (cursor >= length) return false
        if (comment == null) for (rec in recognizers) {
            val match = rec.start(source, cursor)
            if (match < 0) continue
            if (checkpoint != cursor)
                token(checkpoint, cursor, ScannedType.CONTENT)
            token(cursor, cursor + match, ScannedType.COMMENT_START)
            comment = rec
            cursor += match
            checkpoint = cursor
            if (checkpoint >= length)
                addPlaceholders()
            return true
        } else {
            val match = comment!!.end(source, cursor)
            if (match < 0) return false
            token(checkpoint, cursor, ScannedType.COMMENT_BODY)
            token(cursor, cursor + match, ScannedType.COMMENT_END)
            comment = null
            cursor += match
            checkpoint = cursor
            return true
        }
        return false
    }

    /**
     * Checks if the cursor is in quoted state or should exit it.
     * Matches against non-escaped `'`, `"`, `'''`, `"""`. Quote must be closed by the same sequence that opened it
     *
     * If quote matches sets the cursor at the last character of the quote.
     * @return `true` if the cursor is inside a quoted string
     */
    private fun quoteStatus(): Boolean {
        if (comment != null || cursor >= length) return false
        when (source[cursor]) {
            '\'' -> if (source.getOrDefault(cursor - 1) != '\\') when (quote) {
                QuoteType.SINGLE -> quote = null
                QuoteType.DOC_SINGLE -> if (next2Are('\'')) {
                    cursor += 2; quote = null
                }

                null -> if (next2Are('\'')) {
                    cursor += 2; quote = QuoteType.DOC_SINGLE
                } else quote = QuoteType.SINGLE

                else -> {}
            }

            '"' -> if (source.getOrDefault(cursor - 1) != '\\') when (quote) {
                QuoteType.DOUBLE -> quote = null
                QuoteType.DOC_DOUBLE -> if (next2Are('"')) {
                    cursor += 2; quote = null
                }

                null -> if (next2Are('"')) {
                    cursor += 2; quote = QuoteType.DOC_DOUBLE
                } else quote = QuoteType.DOUBLE

                else -> {}
            }
        }
        return quote != null
    }

    private fun token(start: Int, end: Int, type: ScannedType) {
        buffer += SourcedToken(source, start..<end, type)
    }

    private fun next2Are(char: Char): Boolean =
        source.getOrDefault(cursor + 1) == char && source.getOrDefault(cursor + 2) == char
}