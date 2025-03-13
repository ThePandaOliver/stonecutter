@file:UseSerializers(IntRangeSerializer::class)

package dev.kikugie.stwotcher.data.token

import dev.kikugie.stwotcher.data.type.InvalidType
import dev.kikugie.stwotcher.data.type.TokenType
import dev.kikugie.stwotcher.util.IntRangeSerializer
import dev.kikugie.stwotcher.util.shiftBy
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

const val SAME_SOURCE_ERR = "Components must have the same source"

inline fun <reified T : TokenType> StitcherToken?.isOf() = this != null && this.type is T
fun StitcherToken?.isOf(type: TokenType) = this != null && this.type == type
fun StitcherToken?.isOf(vararg types: TokenType) = this != null && types.any(::isOf)

fun StitcherToken.slice(range: IntRange, type: TokenType)
    = SourcedToken(source, range shiftBy this.range.first, type)

inline fun requireSameSource(vararg tokens: StitcherToken, message: () -> String = ::SAME_SOURCE_ERR) =
    requireSameSource(tokens.toList(), message)

inline fun requireSameSource(tokens: Iterable<StitcherToken>, message: () -> String = ::SAME_SOURCE_ERR) =
    require(tokens.isSameSource(), message)

fun Iterable<StitcherToken>.isSameSource(): Boolean {
    var current: StitcherToken? = null
    for (token in this) {
        if (current != null && current.source != token.source)
            return false
        current = token
    }
    return true
}

@Serializable
sealed interface StitcherToken {
    val type: TokenType
    val range: IntRange
    val source: CharSequence

    val value: String get() = source.substring(range)
}

@Serializable
data class SourcedToken(
    override val source: CharSequence,
    override val range: IntRange,
    override val type: TokenType,
) : StitcherToken

@Serializable
data class LiteralToken(
    override val value: String,
    override val type: TokenType,
) : StitcherToken {
    override val range: IntRange get() = value.indices
    override val source: CharSequence get() = value
}

@Serializable
data class PlaceholderToken(
    override val source: CharSequence,
    val index: Int,
) : StitcherToken {
    override val range: IntRange
        get() = index..<index
    override val type: TokenType
        get() = InvalidType
}