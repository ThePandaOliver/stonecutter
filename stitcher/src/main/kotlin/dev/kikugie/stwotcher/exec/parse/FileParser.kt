package dev.kikugie.stwotcher.exec.parse

import dev.kikugie.stwotcher.data.type.ScopeEnclosure
import dev.kikugie.stwotcher.data.token.BlockToken
import dev.kikugie.stwotcher.data.token.DefinitionToken
import dev.kikugie.stwotcher.data.token.StitcherToken
import dev.kikugie.stwotcher.data.type.MarkerType
import dev.kikugie.stwotcher.data.type.ScannedType
import dev.kikugie.stwotcher.data.type.SugarType
import dev.kikugie.stwotcher.exec.issue.FileProblemsBuilder
import dev.kikugie.stwotcher.exec.issue.ProblemID
import dev.kikugie.stwotcher.exec.lex.CommentLexer
import dev.kikugie.stwotcher.exec.parse.LookaheadParser.Companion.advancing
import dev.kikugie.stwotcher.exec.parse.LookaheadParser.Companion.consumeOrPlaceholder
import dev.kikugie.stwotcher.exec.parse.LookaheadParser.Companion.placeholder
import dev.kikugie.stwotcher.util.LookaheadIterator
import dev.kikugie.stwotcher.util.isBlank
import dev.kikugie.stwotcher.util.isOf
import java.util.*
import kotlin.io.path.Path

class FileParser(
    override val source: StitcherToken,
    override val iterator: LookaheadIterator<out StitcherToken>,
) : LookaheadParser {
    val top: BlockToken.Container get() = scopes.peekLast()
    val root: BlockToken.Root = BlockToken.Root()

    override val problems: FileProblemsBuilder = FileProblemsBuilder(Path(""), source.source)
    val scopes: ArrayDeque<BlockToken.Container> = ArrayDeque<BlockToken.Container>()
        .apply { add(root) }

    fun parse(): BlockToken = build().let { root }

    private fun build() {
        while (hasNext()) when (current.type) {
            ScannedType.CONTENT -> advancing(::pushContent)
            ScannedType.COMMENT_START -> advancing(::buildComment)
            else -> error("FileParser has encountered a malformed token $current")
        }.also {
            val isOpen = top is BlockToken.Code && (top as BlockToken.Code).enclosure != ScopeEnclosure.CLOSED
            val hasContent = top.lastOrNull()?.isBlank() == false
            if (isOpen && hasContent) scopes.removeLast()
        }
    }

    private fun buildComment(prev: StitcherToken): BlockToken {
        return if (currentOrNull == null) placeholder()
            .let { pushComment(prev, it, it) }
        else when (current.type) {
            ScannedType.COMMENT_END -> advancing {
                pushComment(prev, placeholder(it.range.first), it)
            }
            ScannedType.COMMENT_BODY -> advancing {
                val def = parseComment(it) ?: return@advancing pushComment(prev, it, consumeOrPlaceholder())
                BlockToken.Code(prev, def, consumeOrPlaceholder()).also { handleNewScope(it) }
            }
            else -> error("FileParser has encountered a malformed token $current")
        }
    }

    private fun parseComment(source: StitcherToken): DefinitionToken? {
        val lexer = CommentLexer(source, limit = 50 /*TODO: Make configurable*/)
        val parser = CommentParser(source, lexer)
        val (def, list) = parser.parse()
        return def.also { problems.storage += list }
    }

    private fun handleNewScope(block: BlockToken.Code) {
        if (block.extension)
            if (block.marker == top.marker) scopes.removeLast()
            else problems.report(ProblemID.INVALID_CLOSER, block.body.range)

        top.add(block)
        if (createsNewScope(block)) scopes.add(block)
    }

    private fun pushContent(token: StitcherToken) =
        BlockToken.Content(token).also(top::add)

    private fun pushComment(open: StitcherToken, body: StitcherToken, close: StitcherToken) =
        BlockToken.Comment(open, body, close).also(top::add)

    private fun createsNewScope(block: BlockToken.Code) = when (block.marker) {
        MarkerType.REPLACEMENT -> false
        MarkerType.SWAP -> !block.extension
        MarkerType.CONDITION -> with(block.body as DefinitionToken.Condition) {
            when {
                closer != null -> true
                expression != null -> true
                sugar.any { it.isOf(SugarType.ELSE)} -> true
                else -> false
            }
        }
    }
}