@file:UseSerializers(IntRangeSerializer::class)

package dev.kikugie.stwotcher.data.token

import dev.kikugie.stwotcher.data.type.InvalidType
import dev.kikugie.stwotcher.data.type.TokenType
import dev.kikugie.stwotcher.util.IntRangeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

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
) : StitcherToken {
    override fun toString(): String = "SourcedToken($type, '$value')"
}

@Serializable
data class LiteralToken(
    override val value: String,
    override val type: TokenType,
) : StitcherToken {
    override val range: IntRange get() = value.indices
    override val source: CharSequence get() = value
    override fun toString(): String = "LiteralToken($type, '$value')"
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
    override val value: String
        get() = ""
    override fun toString(): String = "PlaceholderToken($index)"
}

@Serializable
data class InjectedToken(
    val host: StitcherToken,
    val token: StitcherToken,
    val index: Int,
) : StitcherToken by token