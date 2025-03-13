package dev.kikugie.stwotcher.data.token

import dev.kikugie.stwotcher.data.type.ComponentType
import dev.kikugie.stwotcher.data.type.TokenType
import kotlinx.serialization.Serializable

@Serializable
sealed interface ComponentToken : StitcherToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitConstant(it: Constant): T
        fun visitUnary(it: Unary): T
        fun visitBinary(it: Binary): T
        fun visitGroup(it: Group): T
        fun visitAssignment(it: Assignment): T
    }

    @Serializable @JvmInline
    value class Constant(val token: StitcherToken) : ComponentToken, StitcherToken by token {
        override val type: TokenType get() = ComponentType.CONSTANT

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitConstant(this)
    }
    
    @Serializable
    data class Group(
        val opener: StitcherToken,
        val body: StitcherToken,
        val closer: StitcherToken,
    ) : ComponentToken {
        override val source: CharSequence get() = body.source
        override val range: IntRange get() = opener.range.first..closer.range.last
        override val type: TokenType get() = ComponentType.GROUP
        
        init { requireSameSource(opener, body, closer) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitGroup(this)
    }
    
    @Serializable
    data class Unary(
        val operator: StitcherToken,
        val right: StitcherToken
    ) : ComponentToken {
        override val source: CharSequence get() = right.source
        override val range: IntRange get() = operator.range.first..right.range.last
        override val type: TokenType get() = ComponentType.UNARY
        
        init { requireSameSource(operator, right) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitUnary(this)
    }
    
    @Serializable
    data class Binary(
        val left: StitcherToken,
        val operator: StitcherToken,
        val right: StitcherToken
    ) : ComponentToken {
        override val source: CharSequence get() = left.source
        override val range: IntRange get() = left.range.first..right.range.last
        override val type: TokenType get() = ComponentType.BINARY
        
        init { requireSameSource(left, operator, right) }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitBinary(this)
    }

    @Serializable
    data class Assignment(
        val target: StitcherToken? = null,
        val operator: StitcherToken? = null,
        val predicates: List<StitcherToken> = emptyList(),
    ) : ComponentToken {
        override val source: CharSequence by lazy(::determineSource)
        override val range: IntRange by lazy(::determineRange)
        override val type: TokenType get() = ComponentType.ASSIGNMENT

        init {
            val targets = buildList {
                target?.let(::add)
                operator?.let(::add)
                addAll(predicates)
            }
            require(targets.isNotEmpty()) { "Assignment must have at least one token" }
            requireSameSource(targets)
        }

        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitAssignment(this)

        private fun determineRange(): IntRange {
            val start = target?.range?.start
                ?: operator?.range?.start
                ?: predicates.firstOrNull()?.range?.start
                ?: error("No tokens to determine range for assignment")
            val end = predicates.lastOrNull()?.range?.last
                ?: operator?.range?.last
                ?: target?.range?.last
                ?: error("No tokens to determine range for assignment")
            return start..end
        }

        private fun determineSource(): CharSequence = target?.source ?: operator?.source ?: predicates.firstOrNull()?.source
            ?: error("No tokens to determine source for assignment")
    }
}