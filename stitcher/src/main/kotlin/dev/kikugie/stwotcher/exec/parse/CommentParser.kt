package dev.kikugie.stwotcher.exec.parse

import dev.kikugie.stwotcher.data.token.*
import dev.kikugie.stwotcher.data.type.*
import dev.kikugie.stwotcher.exec.issue.FileProblemsBuilder
import dev.kikugie.stwotcher.exec.issue.ProblemEntry
import dev.kikugie.stwotcher.exec.issue.ProblemID
import dev.kikugie.stwotcher.util.LookaheadIterator
import dev.kikugie.stwotcher.util.merge

private fun StitcherToken?.isCloser() = this == null || isOf(ScopeType.SCOPE_OPEN, ScopeType.EXPECT_WORD)
private fun List<StitcherToken>.range() = first().range.first..last().range.last
private inline fun <T> List<T>.ifNotEmpty(action: (List<T>) -> Unit) = if (isNotEmpty()) action(this) else Unit

class CommentParser(
    val source: SourcedToken,
    val iterator: LookaheadIterator<SourcedToken>,
    val problems: FileProblemsBuilder
) {
    companion object {
        const val STREAM_END = "Reached end of the token stream"
    }

    val index = currentOrNull?.range?.first ?: source.range.last
    val current: SourcedToken get() = checkNotNull(iterator.peek(), ::STREAM_END)
    val currentOrNull: SourcedToken? get() = iterator.peek()

    fun parse(): DefinitionToken? {
        val marker: SourcedToken = if (currentOrNull.isOf<MarkerType>()) consume() else return null
        val extension: SourcedToken? = if (currentOrNull.isOf(ScopeType.SCOPE_CLOSE)) consume() else null

        val body = when (marker.type as MarkerType) {
            MarkerType.CONDITION -> parseCondition(extension != null)
            MarkerType.SWAP -> parseSwap(extension != null)
            MarkerType.REPLACEMENT -> parseReplacement(extension != null)
        }

        val closer = currentOrNull //TODO: At this point the token must be null or a closer
        if (closer != null) consuming {}
        TODO("Report remaining tokens")
        return Component.Definition(marker, extension, body, closer)
    }

    private fun parseSwap(extension: StitcherToken?): StitcherToken {
        if (extension != null && hasNext()) consumeWhile { true }.ifNotEmpty {
            problems.report(ProblemID.UNEXPECTED_EXPRESSION, extension.range merge it.range())
        }
        return parseSingleIdentifier(ReferenceType.SWAP, StitcherToken?::isCloser)
    }

    private fun parseReplacement(extension: StitcherToken?): StitcherToken {
        if (extension != null) consumeWhile { true }.ifNotEmpty {
            problems.report(ProblemID.UNEXPECTED_EXPRESSION, extension.range merge it.range())
        }
        return parseSingleIdentifier(ReferenceType.REPLACEMENT) { true }
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

    private fun parseCondition(extension: StitcherToken?): Pair<List<StitcherToken>, ComponentToken?> {
        val sugar = if (currentOrNull?.type is SugarType) parseSugar(isExtension) else emptyList()
        val needsExpressions = sugar.lastOrNull()?.type.let { it == SugarType.IF || it == SugarType.ELIF }
        val expression = if (currentOrNull != null && !currentOrNull.isCloser()) parseExpression() else EmptySourcedToken(source)
        if (needsExpressions && expression is EmptySourcedToken) TODO("Report missing expression")
        TODO("Report remaining stuff until the closer") //TODO: Also this cast won't succeed
        return Component.Condition(sugar, expression as Component)
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

    private fun parseExpression(): Component = when (current.type) {
        OperatorType.NEGATE -> advancing { Component.Unary(it, parseExpression()) }
            .let(::tryMatchBoolean)

        OperatorType.GROUP_OPEN -> advancing { opener ->
            val body = parseExpression()
            val closer = if (currentOrNull?.type == OperatorType.GROUP_CLOSE) iterator.next()
            else TODO("Report unclosed parenthesis")
            Component.Group(opener, body, closer)
        }.let(::tryMatchBoolean)

        ReferenceType.UNRESOLVED -> advancing {id ->
            if (currentOrNull.isCloser()) Component.UnresolvedAssignment(id)
            else if (current.type == OperatorType.ASSIGN) advancing {
                val predicates = parsePredicates()
                TODO("Report if empty predicates")
                Component.FullAssignment(id, it, predicates).let(::tryMatchBoolean)
            }else {
                TODO("This doesn't mean it is a constant")
                Component.Literal(id).let(::tryMatchBoolean)
            }
        }

        ReferenceType.PREDICATE -> Component.DefaultedAssignment(parsePredicates())
            .let(::tryMatchBoolean)
        else -> TODO("Report invalid token")
    }

    private fun parsePredicates(): List<SliceToken> = buildList {
        while (currentOrNull != null) when (current.type) {
            ReferenceType.UNRESOLVED, ReferenceType.PREDICATE -> consuming(::add)
            else -> break
        }
    }

    private fun tryMatchBoolean(left: Component) = if (currentOrNull?.type !is OperatorType) left else when (current.type) {
        OperatorType.AND, OperatorType.OR -> advancing { operator ->
            if (currentOrNull.isCloser()) TODO("Report missing operand")
            Component.Binary(left, operator, parseExpression())
        }
        else -> left
    }

    private fun consume() = iterator.next()
    private fun hasNext() = iterator.hasNext()

    private fun placeholder(index: Int) = PlaceholderToken(source.source, index)

    private inline fun consumeWhile(condition: (StitcherToken) -> Boolean): List<StitcherToken> = buildList {
        while (hasNext() && condition(current)) consuming(::add)
    }

    private inline fun <T> consuming(action: (StitcherToken) -> T): T =
        action(current).also { iterator.next() }

    private inline fun <T> advancing(action: (StitcherToken) -> T): T =
        current.let { iterator.next(); action(it) }
}