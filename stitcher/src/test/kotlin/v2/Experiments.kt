package v2

import dev.kikugie.stwotcher.exec.scan.CommentScanner
import dev.kikugie.stwotcher.exec.scan.DoubleSlashCommentRecognizer
import dev.kikugie.stwotcher.exec.scan.SlashStarCommentRecognizer
import org.junit.jupiter.api.Test

class Experiments {
    @Test fun experiment() {
        val text = """
            // comment one
            normal text
            "// quoted comment"
            /* // comment inside */
        """.trimIndent()
        val recognizers = listOf(
            DoubleSlashCommentRecognizer,
            SlashStarCommentRecognizer
        )
        val scanner = CommentScanner.create(text, recognizers)
        val tokens = scanner.toList()
        println(tokens)
    }
}