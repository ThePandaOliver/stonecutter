package dev.kikugie.stwotcher.data.token

import dev.kikugie.stwotcher.data.type.BlockType
import dev.kikugie.stwotcher.data.type.TokenType
import kotlinx.serialization.Serializable

@Serializable
sealed interface BlockToken : StitcherToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitContent(it: Content): T
        fun visitComment(it: Comment): T
        fun visitCode(it: Code): T
    }

    @Serializable @JvmInline
    value class Content(val token: StitcherToken) : BlockToken, StitcherToken by token {
        override val type: TokenType get() = BlockType.CONTENT

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitContent(this)
    }

    @Serializable
    data class Comment(
        val start: StitcherToken,
        val body: StitcherToken,
        val end: StitcherToken,
    ) : BlockToken {
        override val source: CharSequence get() = body.source
        override val range: IntRange get() = start.range.first..end.range.last
        override val type: TokenType get() = BlockType.COMMENT

        init { requireSameSource(start, body, end) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitComment(this)
    }

    @Serializable
    data class Code(
        val start: StitcherToken,
        val body: StitcherToken,
        val end: StitcherToken,
        val scope: Nothing? = null, /*TODO: Implement scopes*/
    ) : BlockToken {
        override val source: CharSequence get() = body.source
        override val range: IntRange get() = start.range.first..end.range.last
        override val type: TokenType get() = BlockType.CODE
        
        init { requireSameSource(start, body, end) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitCode(this)
    }
}