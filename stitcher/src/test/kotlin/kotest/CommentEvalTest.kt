package kotest

import dev.kikugie.semver.SemanticVersion
import dev.kikugie.stwotcher.data.param.ProcessParameters
import dev.kikugie.stwotcher.data.token.DefinitionToken
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.data.type.WhitespaceType
import dev.kikugie.stwotcher.exec.eval.ConditionChecker
import dev.kikugie.stwotcher.exec.lex.CommentLexer
import dev.kikugie.stwotcher.exec.parse.CommentParser
import dev.kikugie.stwotcher.util.filter
import dev.kikugie.stwotcher.util.isOf
import dev.kikugie.stwotcher.util.toToken
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

private fun createParser(str: String, parameters: ProcessParameters): CommentParser {
    val source = str.toToken(ScannedType.COMMENT_BODY)
    val iter = CommentLexer.create(source).filter { !it.isOf<WhitespaceType>() }
    return CommentParser(source, iter, parameters)
}

private fun eval(input: String, parameters: ProcessParameters, result: Boolean = false, exception: KClass<out Throwable>? = null) {
    val (def, problems) = createParser(input, parameters).parse()
    val checker = ConditionChecker(parameters)
    def.shouldBeOfType<DefinitionToken.Condition> {
        expression.shouldNotBeNull()
        if (exception != null) shouldThrow(exception) { expression.accept(checker) }
        else expression.accept(checker) shouldBe result
    }
}

class CommentEvalTest : StringSpec({
    "standard eval" {
        val input = "? cns1 && !(!!cns2)"
        val params = buildMap {
            this["cns1"] = true
            this["cns2"] = false
        }

        eval(input, ProcessParameters(constants = params), true)
    }

    "inverted defaulted predicate" {
        val input = "? !:>2"
        val params = buildMap {
            this[""] = SemanticVersion(intArrayOf(1))
        }

        eval(input, ProcessParameters(dependencies = params), true)
    }

    "missing constant" {
        val input = "? cns1 && !(!!cns2)"
        val params = emptyMap<String, Boolean>()
        eval(input, ProcessParameters(constants = params), exception = IllegalArgumentException::class)
    }

    "yeet placeholder" {
        val input = "? cns1 && >>"
        val params = buildMap {
            this["cns1"] = true
        }
        eval(input, ProcessParameters(constants = params), exception = UnsupportedOperationException::class)
    }

    // FIXME: Operators should be evaluated left to right
    "boolean precedence test".config(enabled = false) {
        val input = "? a || b && c || d"
        val params = buildMap {
            this["a"] = false
            this["b"] = false
            this["c"] = true
            this["d"] = true
        }
        eval(input, ProcessParameters(constants = params), true)
    }
})