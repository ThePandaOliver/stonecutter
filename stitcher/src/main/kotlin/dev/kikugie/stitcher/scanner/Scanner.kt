package dev.kikugie.stitcher.scanner

import dev.kikugie.commons.collections.FixedQueue
import dev.kikugie.stitcher.data.token.ContentType
import dev.kikugie.stitcher.data.token.Token
import dev.kikugie.stitcher.data.token.TokenType

class Scanner(private val input: CharSequence) : Iterator<Token> {
    companion object {
        fun factory(input: CharSequence): Iterable<Token> = Iterable { Scanner(input) }
    }

    private val flex = FlexScanner(null).apply {
        reset(input, 0, input.length, 0)
    }
    private val buffer = FixedQueue<Token>(4)
    private var checkpoint = 0

    override fun hasNext(): Boolean = buffer.isNotEmpty() || advance()

    override fun next(): Token = when (buffer.size) {
        0 -> if (advance()) buffer.remove() else throw NoSuchElementException()
        else -> buffer.remove()
    }

    private fun advance(): Boolean {
        if (checkpoint >= input.length) return false
        if (flex.runCatching(FlexScanner::advance).getOrNull() != null)
            consumeTokens() else wrapRemaining()
        return true
    }

    private fun consumeTokens() = if (checkpoint >= 0) {
        token(checkpoint, flex.tokenStart, ContentType.CONTENT)
        token(flex.tokenStart, flex.tokenEnd, ContentType.COMMENT_START)
        checkpoint = -flex.tokenEnd
    } else {
        token(-checkpoint, flex.tokenStart, ContentType.COMMENT)
        token(flex.tokenStart, flex.tokenEnd, ContentType.COMMENT_END)
        checkpoint = flex.tokenEnd
    }

    private fun wrapRemaining() {
        if (checkpoint >= 0) token(checkpoint, input.length, ContentType.CONTENT)
        else token(-checkpoint, input.length, ContentType.COMMENT)
        buffer.add(Token.EMPTY)
        checkpoint = input.length
    }

    private fun token(start: Int, end: Int, type: TokenType) {
        buffer.add(Token(input.substring(start, end), type))
    }
}