package dev.kikugie.stwotcher.exec.lex

import dev.kikugie.stwotcher.data.type.TokenType
import dev.kikugie.stwotcher.util.IntRangeSerializer
import kotlinx.serialization.Serializable

/**Represents a configuration for the next token in [CommentLexer] determined by [TokenMatcher].*/
@Serializable
data class LexerSlice(
    /**Range of the subtoken, guaranteed to be in bounds of [TokenMatcher.source].*/
    val range: @Serializable(with = IntRangeSerializer::class) IntRange,
    /**The expected type of the token.*/
    val type: TokenType,
    /**The next state of the [CommentLexer], assigned after the matched token is consumed.*/
    val state: LexerState,
)