package kotest

import dev.kikugie.stwotcher.data.token.ComponentToken
import dev.kikugie.stwotcher.data.token.DefinitionToken
import dev.kikugie.stwotcher.data.token.PlaceholderToken
import dev.kikugie.stwotcher.data.type.*
import dev.kikugie.stwotcher.exec.issue.ProblemID
import dev.kikugie.stwotcher.exec.lex.CommentLexer
import dev.kikugie.stwotcher.exec.parse.CommentParser
import dev.kikugie.stwotcher.util.filter
import dev.kikugie.stwotcher.util.isOf
import dev.kikugie.stwotcher.util.toToken
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.shouldForAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

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
        problems shouldHaveSize 2
        def.shouldBeOfType<DefinitionToken.Replacement> {
            identifier.shouldBeOfType<PlaceholderToken>()
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

    "disallow identifiers in predicate lists" {
        val (def, problems) = createParser("? >=1 <2 sus").parse()
        problems shouldHaveSize 1
        problems.shouldForAny {
            it.id shouldBe ProblemID.UNEXPECTED_EXPRESSION
        }
    }

    "allow empty assign operator" {
        val (def, problems) = createParser("? const && :sus").parse()
        problems shouldHaveSize 0
        def.shouldBeOfType<DefinitionToken.Condition> {
            sugar shouldHaveSize 0
            expression.shouldBeOfType<ComponentToken.Binary> {
                operator.type shouldBe OperatorType.AND
                operator.value shouldBe "&&"

                left.shouldBeOfType<ComponentToken.Constant> {
                    value shouldBe "const"
                }

                right.shouldBeOfType<ComponentToken.Assignment> {
                    target should beNull()
                    predicates shouldHaveSize 1
                    predicates.first().type shouldBe ReferenceType.PREDICATE
                    predicates.first().value shouldBe "sus"
                }
            }
        }
    }

    "allow identifiers in assigned lists" {
        val (def, problems) = createParser("? mc: alpha").parse()
        problems shouldHaveSize 0
        def.shouldBeOfType<DefinitionToken.Condition> {
            sugar shouldHaveSize 0
            expression.shouldBeOfType<ComponentToken.Assignment> {
                target shouldNotBeNull {
                    type shouldBe ReferenceType.DEPENDENCY
                    value shouldBe "mc"
                }
                predicates shouldHaveSize 1
                predicates.first().type shouldBe ReferenceType.PREDICATE
                predicates.first().value shouldBe "alpha"
            }
        }
    }

    "fill in missing right boolean operand" {
        val (def, problems) = createParser("? const &&").parse()
        problems shouldHaveSize 1
        problems.shouldForAny {
            it.id shouldBe ProblemID.MISSING_PARAMETER
        }
        def.shouldBeOfType<DefinitionToken.Condition> {
            expression.shouldBeOfType<ComponentToken.Binary> {
                right.shouldBeOfType<ComponentToken.Placeholder>()
            }
        }
    }
})