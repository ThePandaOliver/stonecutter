package dev.kikugie.stwotcher.data.token

import dev.kikugie.stwotcher.data.ScopeEnclosure
import dev.kikugie.stwotcher.data.type.BlockType
import dev.kikugie.stwotcher.data.type.MarkerType
import dev.kikugie.stwotcher.data.type.ScopeType
import dev.kikugie.stwotcher.data.type.TokenType
import dev.kikugie.stwotcher.util.requireSameSource
import kotlinx.serialization.Serializable

@Serializable
sealed interface BlockToken : StitcherToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Container : BlockToken, MutableList<BlockToken> {
        val marker: MarkerType?
    }

    interface Visitor<T> {
        fun visitContent(it: Content): T
        fun visitComment(it: Comment): T
        fun visitCode(it: Code): T
        fun visitRoot(it: Root): T
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
        val body: DefinitionToken,
        val end: StitcherToken,
        val scope: MutableList<BlockToken> = mutableListOf(),
    ) : Container, MutableList<BlockToken> by scope {
        override val source: CharSequence get() = body.source
        override val range: IntRange get() = start.range.first..end.range.last
        override val type: TokenType get() = BlockType.CODE
        override val marker: MarkerType get() = body.marker.type as MarkerType

        val extension: Boolean get() = body.extension != null
        val enclosure: ScopeEnclosure get() = when (body.closer?.type as? ScopeType ?: ScopeType.SCOPE_CLOSE) {
            ScopeType.SCOPE_CLOSE -> ScopeEnclosure.LINE // Actually null, but we substitute
            ScopeType.SCOPE_OPEN -> ScopeEnclosure.CLOSED
            ScopeType.EXPECT_WORD -> ScopeEnclosure.WORD
        }
        
        init { requireSameSource(start, body, end) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitCode(this)
    }

    @Serializable @JvmInline
    value class Root(val scope: MutableList<BlockToken> = mutableListOf()) : Container, MutableList<BlockToken> by scope {
        override val source: CharSequence get() = scope.first().source
        override val range: IntRange get() =
            if (scope.isEmpty()) IntRange.EMPTY
            else scope.first().range.first..scope.last().range.last
        override val type: TokenType get() = BlockType.ROOT
        override val marker: MarkerType? get() = null

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitRoot(this)
    }
}