package dev.kikugie.stwotcher.exec.scan

import dev.kikugie.stwotcher.exec.scan.CommentRecognizer.Companion.matchEOL
import dev.kikugie.stwotcher.util.getOrDefault

interface CommentRecognizer {
    fun start(input: CharSequence, offset: Int): Int
    fun end(input: CharSequence, offset: Int): Int

    companion object {
        @JvmStatic fun matchEOL(input: CharSequence, offset: Int) = when(input.getOrDefault(offset)) {
            '\r' -> if (input.getOrDefault(offset + 1) == '\n') 2 else 1
            '\n' -> 1
            else -> -1
        }
    }
}

data object HashCommentRecognizer : CommentRecognizer {
    override fun start(input: CharSequence, offset: Int): Int =
        if (input[offset] == '#') 1 else -1

    override fun end(input: CharSequence, offset: Int): Int =
        matchEOL(input, offset)
}

data object DoubleSlashCommentRecognizer : CommentRecognizer {
    override fun start(input: CharSequence, offset: Int): Int =
        if (input[offset] == '/' && input.getOrDefault(offset + 1) == '/') 2 else -1

    override fun end(input: CharSequence, offset: Int): Int =
        matchEOL(input, offset)
}

data object SlashStarCommentRecognizer : CommentRecognizer {
    override fun start(input: CharSequence, offset: Int): Int =
        if (input[offset] == '/' && input.getOrDefault(offset + 1) == '*') 2 else -1

    override fun end(input: CharSequence, offset: Int): Int =
        if (input[offset] == '*' && input.getOrDefault(offset + 1) == '/') 2 else -1
}