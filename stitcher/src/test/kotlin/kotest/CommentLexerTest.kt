package kotest

import dev.kikugie.stwotcher.data.token.StitcherToken
import dev.kikugie.stwotcher.data.type.MarkerType
import dev.kikugie.stwotcher.data.type.ReferenceType
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.data.type.WhitespaceType
import dev.kikugie.stwotcher.exec.lex.CommentLexer
import dev.kikugie.stwotcher.util.filter
import dev.kikugie.stwotcher.util.isOf
import dev.kikugie.stwotcher.util.toToken
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldNotBe

class CommentLexerTest : StringSpec({
    fun createLexer(str: String) = CommentLexer.create(str.toToken(ScannedType.COMMENT_BODY))

    "whitespace filtering" {
        val haha = "? Is there anyway for Fabric developers to write their Fabric Modding API and modding ability to new Android app by Fabric that will do the work just like Java Edition select Fabric profile"
        val tokens = CommentLexer.create(haha.toToken(ScannedType.COMMENT_BODY)).filter { !it.isOf<WhitespaceType>() }.toList()
        val mapped = tokens.map(StitcherToken::type)
        mapped.isEmpty() shouldNotBe true
        mapped shouldNotContain WhitespaceType
    }


    "regular replacement" {
        val tokens = createLexer("~ identifier").toList()
        val mapped = tokens.map(StitcherToken::type)
        val required = listOf(MarkerType.REPLACEMENT, WhitespaceType, ReferenceType.REPLACEMENT)
        mapped shouldContainExactly required
    }
})