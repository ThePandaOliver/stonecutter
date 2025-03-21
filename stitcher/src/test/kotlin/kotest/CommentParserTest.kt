package kotest

import dev.kikugie.stwotcher.data.token.ComponentToken
import dev.kikugie.stwotcher.data.token.DefinitionToken
import dev.kikugie.stwotcher.data.token.PlaceholderToken
import dev.kikugie.stwotcher.data.type.MarkerType
import dev.kikugie.stwotcher.data.type.ReferenceType
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.data.type.WhitespaceType
import dev.kikugie.stwotcher.exec.issue.ProblemID
import dev.kikugie.stwotcher.exec.lex.CommentLexer
import dev.kikugie.stwotcher.exec.parse.CommentParser
import dev.kikugie.stwotcher.util.filter
import dev.kikugie.stwotcher.util.isOf
import dev.kikugie.stwotcher.util.toToken
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.shouldForAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe

class CommentParserTest : StringSpec({
    fun createParser(str: String): CommentParser {
        val source = str.toToken(ScannedType.COMMENT_BODY)
        val iter = CommentLexer.create(source).filter { !it.isOf<WhitespaceType>() }
        return CommentParser(source, iter)
    }

    "plain string" {
        val (def, problems) = createParser("just a string").parse()
        problems shouldHaveSize 0
        def shouldBe null
    }

    "offset marker" {
        val (def, problems) = createParser(" ? oh no a space").parse()
        problems shouldHaveSize 0
        def shouldBe null
    }

    "regular replacement" {
        val (def, problems) = createParser("~ identifier").parse()
        problems shouldHaveSize 0
        def.shouldBeOfType<DefinitionToken.Replacement> {
            marker.type shouldBe MarkerType.REPLACEMENT
            marker.value shouldBe "~"

            identifier.type shouldBe ReferenceType.REPLACEMENT
            identifier.value shouldBe "identifier"
        }
    }

    "duplicate replacement" {
        val text = "~ identifier evil_twin"
        val (def, problems) = createParser(text).parse()
        problems.shouldForAny {
            text.substring(it.range) shouldBe "evil_twin"
        }

        def.shouldBeOfType<DefinitionToken.Replacement> {
            identifier.type shouldBe ReferenceType.REPLACEMENT
            identifier.value shouldBe "identifier"
        }
    }

    "malformed replacement" {
        val (def, problems) = createParser("~} identifier").parse()
        problems shouldHaveSize 0

        def.shouldBeOfType<DefinitionToken.Replacement> {
            identifier.shouldBe<PlaceholderToken>()
        }
    }

    "regular swap" {
        val (def, problems) = createParser("$ identifier").parse()
        problems shouldHaveSize 0
        def.shouldBeOfType<DefinitionToken.Swap> {
            marker.type shouldBe MarkerType.SWAP
            marker.value shouldBe "$"

            identifier.shouldNotBeNull()
            identifier.type shouldBe ReferenceType.SWAP
            identifier.value shouldBe "identifier"
        }
    }

    "unexpected swap id" {
        val text = "$} identifier"
        val (def, problems) = createParser(text).parse()
        problems shouldHaveSize 1
        problems.shouldForAny {
            text.substring(it.range) shouldBe "} identifier"
        }
    }

    "expected empty swap" {
        val text = "$}"
        val (def, problems) = createParser(text).parse()
        problems shouldHaveSize 0
        def.shouldBeOfType<DefinitionToken.Swap> {
            identifier should beNull()
        }
    }

    "regular condition" {
        val (def, problems) = createParser("? identifier").parse()
        problems shouldHaveSize 0
        def.shouldBeOfType<DefinitionToken.Condition> {
            marker.type shouldBe MarkerType.CONDITION
            marker.value shouldBe "?"

            sugar shouldHaveSize 0
            expression.shouldBeOfType<ComponentToken.Constant> {
                value shouldBe "identifier"
            }
        }
    }

    "missing required expression" {
        val (def, problems) = createParser("? if").parse()
        problems shouldHaveSize 1
        problems.shouldForAny {
            it.id shouldBe ProblemID.MISSING_PARAMETER
        }
        def.shouldBeOfType<DefinitionToken.Condition> {
            sugar shouldHaveSize 1
            expression should beNull()
        }
    }

    // TODO: Writing tests is boring - future me do it
})