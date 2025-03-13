package dev.kikugie.stwotcher.data.token

import dev.kikugie.stwotcher.data.type.DefinitionType
import dev.kikugie.stwotcher.data.type.TokenType
import kotlinx.serialization.Serializable

@Serializable
sealed interface DefinitionToken : StitcherToken {
    val marker: SourcedToken
    val extension: SourcedToken?
    val closer: SourcedToken?

    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitSwap(it: Swap): T
        fun visitReplacement(it: Replacement): T
        fun visitCondition(it: Condition): T
    }

    @Serializable
    data class Swap(
        override val marker: SourcedToken,
        override val extension: SourcedToken? = null,
        val identifier: SourcedToken? = null,
        override val closer: SourcedToken? = null
    ): DefinitionToken {
        override val source: CharSequence get() = marker.source
        override val range: IntRange by lazy(::determineRange)
        override val type: TokenType get() = DefinitionType.SWAP

        init { requireSameSource(listOfNotNull(marker, extension, identifier, closer)) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitSwap(this)

        private fun determineRange(): IntRange {
            val end = closer?.range?.last
                ?: identifier?.range?.last
                ?: extension?.range?.last
                ?: marker.range.last
            return marker.range.first..end
        }
    }

    @Serializable
    data class Replacement(
        override val marker: SourcedToken,
        val identifier: SourcedToken,
    ): DefinitionToken {
        override val extension: SourcedToken? get() = null
        override val closer: SourcedToken? get() = null

        override val source: CharSequence get() = marker.source
        override val range: IntRange get() = marker.range.first..identifier.range.last
        override val type: TokenType get() = DefinitionType.REPLACEMENT

        init { requireSameSource(marker, identifier) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitReplacement(this)
    }

    @Serializable
    data class Condition(
        override val marker: SourcedToken,
        override val extension: SourcedToken? = null,
        val sugar: List<SourcedToken> = emptyList(),
        val expression: ComponentToken? = null,
        override val closer: SourcedToken? = null
    ): DefinitionToken {
        override val source: CharSequence get() = marker.source
        override val range: IntRange by lazy(::determineRange)
        override val type: TokenType get() = DefinitionType.CONDITION

        init { requireSameSource(listOfNotNull(marker, extension, expression, closer) + sugar) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitCondition(this)

        private fun determineRange(): IntRange {
            val end = closer?.range?.last
                ?: expression?.range?.last
                ?: sugar.lastOrNull()?.range?.last
                ?: extension?.range?.last
                ?: marker.range.last
            return marker.range.first..end
        }
    }
}