package kotest

import dev.kikugie.stwotcher.data.token.LiteralToken
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.exec.scan.*
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

@Serializable
private class ScannerTestSpec(val input: String, val tokens: List<String>, val recognizers: List<String> = listOf("hash", "slash", "star")) : DataTestInstance {
    override fun display(): String = buildString {
        appendLine("Input: $input")
        appendLine("Tokens: ${tokens.joinToString("\n") { "  $it" }}")
    }

    override fun execute(scope: TestScope) {
        val scanned = CommentScanner.iterable(input, recognizers.map { it.parseAsRecognizer() })
            .map { LiteralToken(it.value, it.type) }
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

private inline fun <T> Iterable<T>.peekEach(block: (T, T?) -> Unit) = with(toList()) {
    for (i in indices) block(get(i), getOrNull(i + 1))
}

@Tags("Stitcher", "Scanner")
class CommentScannerTest : StringSpec({
    withData<ScannerTestSpec>("scanner.yml")

    withData(mapOf(
        "retain empty body" to "/**/",
        "insert comment end" to "// body",
        "insert both parts" to "hey /*"
    )) {
        CommentScanner.iterable(it, listOf(SlashStarCommentRecognizer)).peekEach { token, next ->
            when (token.type as ScannedType) {
                ScannedType.COMMENT_START -> next shouldNotBeNull {
                    type shouldBe ScannedType.COMMENT_BODY
                }
                ScannedType.COMMENT_BODY -> next shouldNotBeNull {
                    type shouldBe ScannedType.COMMENT_END
                }
                else -> { /* ok */ }
            }
        }
    }
})