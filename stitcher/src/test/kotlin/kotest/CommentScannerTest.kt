package kotest

import dev.kikugie.stwotcher.data.token.LiteralToken
import dev.kikugie.stwotcher.data.token.SourcedToken
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.exec.scan.CommentRecognizer
import dev.kikugie.stwotcher.exec.scan.CommentScanner
import dev.kikugie.stwotcher.exec.scan.DoubleSlashCommentRecognizer
import dev.kikugie.stwotcher.exec.scan.HashCommentRecognizer
import dev.kikugie.stwotcher.exec.scan.SlashStarCommentRecognizer
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.should
import kotlinx.serialization.Serializable

@Serializable
private class ScannerTestSpec(val input: String, val tokens: List<String>, val recognizers: List<String> = listOf("hash", "slash", "star")) : DataTestInstance {
    override fun display(): String = buildString {
        appendLine("Input: $input")
        appendLine("Tokens: ${tokens.joinToString("\n") { "  $it" }}")
    }

    override fun execute(scope: TestScope) {
        val scanned = CommentScanner.create(input, recognizers.map { it.parseAsRecognizer() })
            .map(SourcedToken::asLiteral)
        val expected = tokens.map { it.parseAsToken() }
        scanned shouldContainExactly expected
    }

    private fun String.parseAsToken(): LiteralToken {
        val index = withClue("Missing token type separator") {
            indexOfLast { it == ';' } shouldBeGreaterThanOrEqual 0
        }

        val value = substring(0, index)
        val type = withClue("Invalid token type") {
            substring(index + 1).trim().uppercase().let(ScannedType::valueOf)
        }

        return LiteralToken(value, type)
    }

    private fun String.parseAsRecognizer(): CommentRecognizer = when (this) {
        "hash" -> HashCommentRecognizer
        "slash" -> DoubleSlashCommentRecognizer
        "star" -> SlashStarCommentRecognizer
        else -> withClue("Invalid recognizer type") {
            this shouldBeOneOf listOf("hash", "slash", "star")
            error("Unreachable")
        }
    }
}

@Tags("Stitcher", "Scanner")
class CommentScannerTest : StringSpec({
    withData<ScannerTestSpec>("scanner.yml")
})