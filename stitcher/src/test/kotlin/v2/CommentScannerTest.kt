package v2

import com.github.ajalt.mordant.rendering.TextColors
import dev.kikugie.stwotcher.data.token.LiteralToken
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.data.type.ScannedType.COMMENT_BODY
import dev.kikugie.stwotcher.data.type.ScannedType.COMMENT_END
import dev.kikugie.stwotcher.data.type.ScannedType.COMMENT_START
import dev.kikugie.stwotcher.data.type.ScannedType.CONTENT
import dev.kikugie.stwotcher.exec.scan.CommentRecognizer
import dev.kikugie.stwotcher.exec.scan.CommentScanner
import dev.kikugie.stwotcher.exec.scan.DoubleSlashCommentRecognizer
import dev.kikugie.stwotcher.exec.scan.HashCommentRecognizer
import dev.kikugie.stwotcher.exec.scan.SlashStarCommentRecognizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestFactory

class CommentScannerTest {
    @TestFactory
    fun `test recognizers`() = test<ScannerTestInstance> {
        "hash comment" {
            input = "# comment"
            token("#", COMMENT_START)
            token(" comment", COMMENT_BODY)
        }

        "slash comment" {
            input = "// comment"
            token("//", COMMENT_START)
            token(" comment", COMMENT_BODY)
        }

        "slash star comment" {
            input = "/* comment */"
            token("/*", COMMENT_START)
            token(" comment ", COMMENT_BODY)
            token("*/", COMMENT_END)
        }

        "doc comment" {
            input = "/**\n* comment\n*/"
            token("/*", COMMENT_START)
            token("*\n* comment\n", COMMENT_BODY)
            token("*/", COMMENT_END)
        }
    }

    @TestFactory
    fun `test nesting`() = test<ScannerTestInstance> {
        "open comment" {
            input = "/* comment"
            token("/*", COMMENT_START)
            token(" comment", COMMENT_BODY)
        }

        "nested slash-slash comment" {
            input = "// comment // comment"
            token("//", COMMENT_START)
            token(" comment // comment", COMMENT_BODY)
        }

        "nested slash-hash comment" {
            input = "// comment # comment"
            token("//", COMMENT_START)
            token(" comment # comment", COMMENT_BODY)
        }

        "nested multi-multi comment" {
            input = "/* comm /* comment */ ent */"
            token("/*", COMMENT_START)
            token(" comm /* comment ", COMMENT_BODY)
            token("*/", COMMENT_END)
            token(" ent */", CONTENT)
        }

        "nested slash-multi comment" {
            input = "// comm /* comment */ ent"
            token("//", COMMENT_START)
            token(" comm /* comment */ ent", COMMENT_BODY)
        }

        "nested multi-slash comment" {
            input = "/* comm // ent */"
            token("/*", COMMENT_START)
            token(" comm // ent ", COMMENT_BODY)
            token("*/", COMMENT_END)
        }
    }

    @TestFactory
    fun `quote comments`() = test<ScannerTestInstance> {
        "quote in comment" {
            input = "// \"cool\" comment"
            token("//", COMMENT_START)
            token(" \"cool\" comment", COMMENT_BODY)
        }

        "comment in quote" {
            input = "\"nice // quote\""
            token("\"nice // quote\"", CONTENT)
        }

        "invalid comment quote" {
            input = "/* comm \"ent */ wtf\""
            token("/*", COMMENT_START)
            token(" comm \"ent ", COMMENT_BODY)
            token("*/", COMMENT_END)
            token(" wtf\"", CONTENT)
        }

        "invalid quote comment" {
            input = "\"quote /* comm\" ent*/"
            token("\"quote /* comm\" ent*/", CONTENT)
        }

        "single quote in doubles" {
            input = "\" quote '\""
            token("\" quote '\"", CONTENT)
        }

        "double quote in singles" {
            input = "' quote \"'"
            token("' quote \"'", CONTENT)
        }

        "double quote in doc" {
            input = "\"\"\" still \"quote\" \"\"\""
            token("\"\"\" still \"quote\" \"\"\"", CONTENT)
        }

        "escaped quote" {
            input = "\" quote \\\" \""
            token("\" quote \\\" \"", CONTENT)
        }
    }

    class ScannerTestInstance(override val name: String) : TestInstance {
        lateinit var input: String
        var recognizers: List<CommentRecognizer> = listOf(
            HashCommentRecognizer,
            DoubleSlashCommentRecognizer,
            SlashStarCommentRecognizer
        )
        val expected: MutableList<LiteralToken> = mutableListOf()
        override val display
            get() = buildString {
                appendLine(TextColors.brightCyan("Scanner test: '$name'"))
                appendLine(TextColors.cyan("  Input: '${input.replace("\n", "\\n")}'"))
                appendLine(TextColors.cyan("  Expected:"))
                expected.yaml().lineSequence().forEach { appendLine(TextColors.green("    $it")) }
            }

        fun token(value: String, type: ScannedType) {
            expected += LiteralToken(value, type)
        }

        override fun run() {
            val expected = expected.yaml()
            val scanned = CommentScanner.Supplier(input, recognizers)
                .map { it.literal() }.yaml()
            assertEquals(expected, scanned)
        }
    }
}