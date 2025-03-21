package dev.kikugie.stwotcher.exec.parse

import dev.kikugie.stwotcher.data.token.*
import dev.kikugie.stwotcher.data.type.*
import dev.kikugie.stwotcher.exec.issue.FileProblemsBuilder
import dev.kikugie.stwotcher.exec.issue.ProblemEntry
import dev.kikugie.stwotcher.exec.issue.ProblemID
import dev.kikugie.stwotcher.exec.parse.LookaheadParser.Companion.advancing
import dev.kikugie.stwotcher.exec.parse.LookaheadParser.Companion.consumeWhile
import dev.kikugie.stwotcher.exec.parse.LookaheadParser.Companion.placeholder
import dev.kikugie.stwotcher.util.LookaheadIterator
import dev.kikugie.stwotcher.util.isOf
import dev.kikugie.stwotcher.util.merge
import dev.kikugie.stwotcher.util.remap
import dev.kikugie.stwotcher.util.takeAs
import kotlin.io.path.Path

private fun StitcherToken?.isCloser() = this == null || isOf(ScopeType.SCOPE_OPEN, ScopeType.EXPECT_WORD)
private fun StitcherToken?.isExpressionPart() =
    this != null && isOf(OperatorType.NEGATE, OperatorType.GROUP_OPEN, OperatorType.ASSIGN, ReferenceType.UNRESOLVED, ReferenceType.PREDICATE)

private fun List<StitcherToken>.range() = first().range.first..last().range.last
private inline fun <T> List<T>.ifNotEmpty(action: (List<T>) -> Unit) = if (isNotEmpty()) action(this) else Unit

class CommentParser(
    override val source: StitcherToken,
    override val iterator: LookaheadIterator<out StitcherToken>,
) : LookaheadParser {
    companion object {
        const val STREAM_END = "Reached end of the token stream"
    }

    data class Result(val definition: DefinitionToken?, val problems: List<ProblemEntry>)

    override val problems: FileProblemsBuilder = FileProblemsBuilder(Path(""), source.source)

    fun parse(): Result {
        val marker: StitcherToken = if (currentOrNull.isOf<MarkerType>()) consume() else
            return Result(null, problems.storage)
        val extension: StitcherToken? = if (currentOrNull.isOf(ScopeType.SCOPE_CLOSE)) consume() else null

        val body = when (marker.type as MarkerType) {
            MarkerType.CONDITION -> parseCondition(extension)
            MarkerType.SWAP -> parseSwap(extension)
            MarkerType.REPLACEMENT -> parseReplacement(extension)
        }

        fun reportRemaining() = consumeWhile { true }.ifNotEmpty {
            problems.report(ProblemID.UNEXPECTED_EXPRESSION, it.range())
        }

        val closer = when {
            currentOrNull == null -> null
            current.isCloser() -> advancing { reportRemaining(); it }
            else -> placeholder(index).also { reportRemaining() }
        }

        return Result(body.build(marker, extension, closer), problems.storage)
    }

    private fun parseSwap(extension: StitcherToken?): DefinitionBody = when (extension) {
        null -> parseSingleIdentifier(ReferenceType.SWAP, StitcherToken?::isCloser).let(DefinitionBody::swap)
        else -> {
            if (hasNext()) consumeWhile { true }.ifNotEmpty {
                problems.report(ProblemID.UNEXPECTED_EXPRESSION, extension.range merge it.range())
            }
            DefinitionBody.swap(null)
        }
    }

    private fun parseReplacement(extension: StitcherToken?): DefinitionBody = when (extension) {
        null -> parseSingleIdentifier(ReferenceType.REPLACEMENT) { true }.let(DefinitionBody::replacement)
        else -> {
            consumeWhile { true }.ifNotEmpty {
                problems.report(ProblemID.UNEXPECTED_EXPRESSION, extension.range merge it.range())
            }
            DefinitionBody.replacement(placeholder())
        }
    }

    private fun parseSingleIdentifier(type: TokenType, condition: (StitcherToken) -> Boolean): StitcherToken {
        var identifier: StitcherToken = placeholder(index)
        fun reportRemaining() = consumeWhile(condition).ifNotEmpty {
            problems.report(ProblemID.UNEXPECTED_EXPRESSION, it.range())
        }

        if (currentOrNull?.type == type) advancing { identifier = it; reportRemaining() }
        else problems.report(ProblemID.MISSING_PARAMETER, identifier)
        reportRemaining()

        return identifier
    }

    private fun parseCondition(extension: StitcherToken?): DefinitionBody {
        val sugar = if (currentOrNull.isOf<SugarType>()) parseSugar(extension) else emptyList()
        val cond = sugar.lastOrNull().isOf(SugarType.IF, SugarType.ELIF)
        val expr = if (currentOrNull.isExpressionPart()) parseExpression()
        else if (cond) placeholder(ProblemID.MISSING_PARAMETER)
        else placeholder()

        return DefinitionBody.condition(sugar, expr.takeAs())
    }

    private fun parseSugar(extension: StitcherToken?): List<StitcherToken> = when (current.type as SugarType) {
        SugarType.IF -> {
            if (extension != null) problems.report(ProblemID.UNEXPECTED_EXPRESSION, current.range)
            advancing(::listOf)
        }

        SugarType.ELIF -> {
            if (extension == null) problems.report(ProblemID.UNEXPECTED_EXPRESSION, current.range)
            advancing(::listOf)
        }

        SugarType.ELSE -> {
            if (extension == null) problems.report(ProblemID.UNEXPECTED_EXPRESSION, current.range)
            advancing { els -> if (currentOrNull?.type == SugarType.IF) advancing { listOf(els, it) } else listOf(els) }
        }
    }.also {
        consumeWhile { it.isOf<SugarType>() }.ifNotEmpty {
            problems.report(ProblemID.UNEXPECTED_EXPRESSION, it.range())
        }
    }

    // TODO: Remap and verify tokens
    private fun parseExpression(): ComponentToken = when (current.type) {
        OperatorType.NEGATE -> advanceMatchingBoolean {
            ComponentToken.Unary(it, parseNextExpression())
        }

        OperatorType.GROUP_OPEN -> advanceMatchingBoolean {
            ComponentToken.Group(it, parseNextExpression(), parseNextToken(OperatorType.GROUP_CLOSE))
        }

        OperatorType.ASSIGN -> advanceMatchingBoolean {
            ComponentToken.Assignment(operator = it, predicates = collectPredicates(ReferenceType.UNRESOLVED, ReferenceType.PREDICATE))
        }

        ReferenceType.PREDICATE -> ComponentToken.Assignment(predicates = collectPredicates(ReferenceType.PREDICATE))
            .let(::tryMatchBoolean)

        ReferenceType.UNRESOLVED -> advanceMatchingBoolean { id ->
            if (!currentOrNull.isOf(OperatorType.ASSIGN))
                ComponentToken.Constant(id.remap(ReferenceType.CONSTANT))
            else advancing {
                val predicates = collectPredicates(ReferenceType.UNRESOLVED, ReferenceType.PREDICATE)
                if (predicates.isEmpty()) problems.report(ProblemID.MISSING_PARAMETER, it)
                ComponentToken.Assignment(id.remap(ReferenceType.DEPENDENCY), it, predicates)
            }
        }

        else -> error("CommentParser has encountered a malformed token $current")
    }

    private fun collectPredicates(vararg allowed: TokenType): List<StitcherToken> = buildList {
        while (current.isOf(*allowed)) this += consume().remap(ReferenceType.PREDICATE)
    }

    private fun tryMatchBoolean(left: ComponentToken): ComponentToken =
        if (!currentOrNull.isOf(OperatorType.AND, OperatorType.OR)) left
        else advancing { operator ->
            ComponentToken.Binary(left, operator, parseNextExpression())
        }

    private fun advanceMatchingBoolean(action: (StitcherToken) -> ComponentToken): ComponentToken =
        advancing { action(it) }.let(::tryMatchBoolean)

    private fun parseNextExpression() = parseNextParameter(StitcherToken?::isExpressionPart)
    private fun parseNextToken(vararg allowed: TokenType) = parseNextParameter { it.isOf(*allowed) }

    private inline fun parseNextParameter(condition: (StitcherToken?) -> Boolean) =
        if (condition(currentOrNull)) parseExpression()
        else placeholder(ProblemID.MISSING_PARAMETER)
}