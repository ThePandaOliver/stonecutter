package kotest

import dev.kikugie.stwotcher.data.type.MarkerType
import dev.kikugie.stwotcher.exec.lex.TokenMatcher
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Tags("Stitcher", "Misc")
class PrefixPermanenceTest : StringSpec({
    "condition must be ?" {
        MarkerType.CONDITION.prefix shouldBe '?'
        TokenMatcher.create("?").match().type shouldBe MarkerType.CONDITION
    }

    "swap must be $" {
        MarkerType.SWAP.prefix shouldBe '$'
        TokenMatcher.create("$").match().type shouldBe MarkerType.SWAP
    }

    "replacement must be ~" {
        MarkerType.REPLACEMENT.prefix shouldBe '~'
        TokenMatcher.create("~").match().type shouldBe MarkerType.REPLACEMENT
    }
})