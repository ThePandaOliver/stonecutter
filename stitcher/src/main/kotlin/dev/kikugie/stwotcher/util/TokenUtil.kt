package dev.kikugie.stwotcher.util

import dev.kikugie.stwotcher.data.token.LiteralToken
import dev.kikugie.stwotcher.data.token.PlaceholderToken
import dev.kikugie.stwotcher.data.token.SourcedToken
import dev.kikugie.stwotcher.data.token.StitcherToken
import dev.kikugie.stwotcher.data.type.TokenType

fun CharSequence.toToken(type: TokenType) = SourcedToken(this, 0 until length, type)

inline fun <reified T : TokenType> StitcherToken?.isOf() = this != null && this.type is T
fun StitcherToken?.isOf(type: TokenType) = this != null && this.type == type
fun StitcherToken?.isOf(vararg types: TokenType) = this != null && types.any(::isOf)

fun StitcherToken.slice(range: IntRange, type: TokenType): SourcedToken =
    SourcedToken(source, range shiftBy this.range.first, type)

fun StitcherToken.remap(type: TokenType) = when (this) {
    is SourcedToken -> SourcedToken(source, range, type)
    is LiteralToken -> LiteralToken(value, type)
    else -> throw UnsupportedOperationException("Can't remap $this")
}

fun StitcherToken.isBlank() = value.isBlank()
fun StitcherToken.isNotBlank() = value.isNotBlank()

val StitcherToken.isPlaceholder get() = this is PlaceholderToken

const val SAME_SOURCE_ERR = "Components must have the same source"

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

