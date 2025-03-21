package dev.kikugie.stwotcher.data.type

import kotlinx.serialization.Serializable

@Serializable
sealed interface TokenType

@Serializable
data object WhitespaceType : TokenType

@Serializable
data object InvalidType : TokenType

@Serializable
enum class ScannedType : TokenType {
    COMMENT_START, COMMENT_END, COMMENT_BODY, CONTENT,
}

@Serializable
enum class MarkerType(val prefix: Char) : TokenType {
    CONDITION('?'), SWAP('$'), REPLACEMENT('~');
}

@Serializable
enum class ScopeType : TokenType {
    SCOPE_OPEN, SCOPE_CLOSE, EXPECT_WORD,
}

@Serializable
enum class ReferenceType : TokenType {
    UNRESOLVED, SWAP, REPLACEMENT, PREDICATE, CONSTANT, DEPENDENCY
}

@Serializable
enum class OperatorType : TokenType {
    GROUP_OPEN, GROUP_CLOSE, NEGATE, ASSIGN, AND, OR,
}

@Serializable
enum class SugarType : TokenType {
    IF, ELSE, ELIF,
}

@Serializable
enum class BlockType : TokenType {
    CONTENT, COMMENT, CODE, ROOT,
}

@Serializable
enum class ComponentType : TokenType {
    CONSTANT, UNARY, BINARY, GROUP, ASSIGNMENT,
}

@Serializable
enum class DefinitionType : TokenType {
    CONDITION, SWAP, REPLACEMENT,
}