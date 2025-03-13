package v2

import com.github.ajalt.mordant.rendering.TextColors
import dev.kikugie.stwotcher.data.type.MarkerType
import dev.kikugie.stwotcher.data.type.OperatorType
import dev.kikugie.stwotcher.data.type.ReferenceType
import dev.kikugie.stwotcher.exec.lex.LexerState
import dev.kikugie.stwotcher.exec.lex.TokenMatcher
import dev.kikugie.stwotcher.exec.lex.LexerSlice
import dev.kikugie.stwotcher.util.extend
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestFactory
import kotlin.reflect.KClass

class TokenMatcherTest {
    @TestFactory
    fun `test matcher`() = test<MatcherTestInstance> {
        val template = "  ?$~ if"

        "condition marker init" {
            require('?' == MarkerType.CONDITION.prefix) { "Condition marker is not '?'" }
            input = template
            expected = LexerSlice(template rangeOf "?", MarkerType.CONDITION, LexerState.CONDITION)
        }

        "swap marker init" {
            require('$' == MarkerType.SWAP.prefix) { "Swap marker is not '$'" }
            input = template
            expected = LexerSlice(template rangeOf "$", MarkerType.SWAP, LexerState.SWAP)
        }

        "replacement marker init" {
            require('~' == MarkerType.REPLACEMENT.prefix) { "Replacement marker is not '~'" }
            input = template
            expected = LexerSlice(template rangeOf "~", MarkerType.REPLACEMENT, LexerState.REPLACEMENT)
        }

        "sugar in condition" {
            input = template
            initial = LexerState.CONDITION
            expected = LexerSlice(template rangeOf "if", OperatorType.IF, LexerState.CONDITION)
        }

        "sugar as identifier" {
            input = template
            initial = LexerState.SWAP
            expected = LexerSlice(template rangeOf "if", ReferenceType.SWAP, LexerState.SWAP)
        }
    }

    infix fun String.rangeOf(other: String) = indexOf(other).let {
        require(it != -1) { "'$other' is not found in '$this'" }
        it extend other.length
    }

    class MatcherTestInstance(override val name: String) : TestInstance {
        lateinit var input: String
        lateinit var expected: LexerSlice
        var initial: LexerState = LexerState.UNDEFINED
        var error: KClass<out Throwable>? = null
        var limit: Int = -1
        override val display
            get() = buildString {
                appendLine(TextColors.brightCyan("Matcher test: '$name'"))
                appendLine(TextColors.cyan("  Input: '${input.replace("\n", "\\n")}'"))
                appendLine(TextColors.cyan("  Expected: ${input.substring(expected.range)}"))
                appendLine(TextColors.green("    $expected"))
            }

        inline fun <reified T : Throwable> error() { error = T::class }

        override fun run() {
            val match = if (error == null) match()
            else { assertThrows(error!!.java, ::match); return }

            val expectedStr = expected.yaml()
            val actualStr = match.yaml()
            assertEquals(expectedStr, actualStr) { "Incorrect match for $name" }
        }

        private fun match(): LexerSlice = matcher().match(expected.range.first, initial)

        private fun matcher() = TokenMatcher.create(input)
    }
}
