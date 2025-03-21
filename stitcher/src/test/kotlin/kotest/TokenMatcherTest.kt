package kotest

import dev.kikugie.stwotcher.exec.lex.LexerState
import dev.kikugie.stwotcher.exec.lex.TokenMatcher
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

@Serializable
private class MatcherTestSpec(val input: String, val offset: Int = 0, val state: String = "UNDEFINED", val type: String) : DataTestInstance {
    override fun display(): String = buildString {
        appendLine("Input: $input")
        if (offset != 0) appendLine("Offset: $offset")
        appendLine("Type: $type")
    }

    override fun execute(scope: TestScope) {
        val initial = LexerState.valueOf(state)
        val match = TokenMatcher.create(input).match(offset, initial)
        withClue("Incorrect matched type") {
            match.type.toString() shouldBe type
        }
    }
}

class TokenMatcherTest: StringSpec({
    withData<MatcherTestSpec>("matcher.yml")
})