package dev.kikugie.stwotcher.exec.lex

/**Defines modes for lexer to operate in.*/
enum class LexerState {
    /**Lexing has not started yet, and it's yet to be decided, what type of comment it is.*/
    UNDEFINED,
    /**The comment doesn't contain any Stitcher code and won't be parsed.*/
    PLAIN,
    /**The lexer has encountered a scope marker, and the rest will not be parsed to avoid performance issues.*/
    DONE,
    /**The lexer was assigned the condition mode, and will be able to resolve condition sugar and operators.*/
    CONDITION,
    /**The lexer was assigned the swap mode, and will only resolve identifiers and scope markers.*/
    SWAP,
    /**The lexer was assigned the replacement mode, and will only resolve identifiers and scope markers.*/
    REPLACEMENT,
}