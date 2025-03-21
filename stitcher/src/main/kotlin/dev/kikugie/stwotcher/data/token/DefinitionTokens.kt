package dev.kikugie.stwotcher.data.token

import dev.kikugie.stwotcher.data.type.DefinitionType
import dev.kikugie.stwotcher.data.type.TokenType
import dev.kikugie.stwotcher.util.requireSameSource
import kotlinx.serialization.Serializable

fun interface DefinitionBody {
    fun build(marker: StitcherToken, extension: StitcherToken?, closer: StitcherToken?): DefinitionToken
    
    companion object {
        fun swap(identifier: StitcherToken?) = DefinitionBody { m, e, c ->
            DefinitionToken.Swap(m, e, identifier, c)
        }
        
        fun replacement(identifier: StitcherToken) = DefinitionBody { m, e, c ->
            DefinitionToken.Replacement(m, identifier)
        }
        
        fun condition(sugar: List<StitcherToken>, expression: ComponentToken?) = DefinitionBody { m, e, c ->
            DefinitionToken.Condition(m, e, sugar, expression, c)
        }
    }
}

@Serializable
sealed interface DefinitionToken : StitcherToken {
    val marker: StitcherToken
    val extension: StitcherToken?
    val closer: StitcherToken?

    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitSwap(it: Swap): T
        fun visitReplacement(it: Replacement): T
        fun visitCondition(it: Condition): T
    }

    @Serializable
    data class Swap(
        override val marker: StitcherToken,
        override val extension: StitcherToken? = null,
        val identifier: StitcherToken? = null,
        override val closer: StitcherToken? = null
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
        override val marker: StitcherToken,
        val identifier: StitcherToken,
    ): DefinitionToken {
        override val extension: StitcherToken? get() = null
        override val closer: StitcherToken? get() = null

        override val source: CharSequence get() = marker.source
        override val range: IntRange get() = marker.range.first..identifier.range.last
        override val type: TokenType get() = DefinitionType.REPLACEMENT

        init { requireSameSource(marker, identifier) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitReplacement(this)
    }

    @Serializable
    data class Condition(
        override val marker: StitcherToken,
        override val extension: StitcherToken? = null,
        val sugar: List<StitcherToken> = emptyList(),
        val expression: ComponentToken? = null,
        override val closer: StitcherToken? = null
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