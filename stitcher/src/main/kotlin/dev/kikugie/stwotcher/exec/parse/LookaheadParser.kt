package dev.kikugie.stwotcher.exec.parse

import dev.kikugie.stwotcher.data.token.PlaceholderToken
import dev.kikugie.stwotcher.data.token.StitcherToken
import dev.kikugie.stwotcher.exec.issue.FileProblemsBuilder
import dev.kikugie.stwotcher.exec.issue.ProblemID
import dev.kikugie.stwotcher.exec.parse.CommentParser.Companion.STREAM_END
import dev.kikugie.stwotcher.util.LookaheadIterator

interface LookaheadParser {
    val source: StitcherToken
    val iterator: LookaheadIterator<out StitcherToken>
    val problems: FileProblemsBuilder

    val index get() = currentOrNull?.range?.first ?: (source.range.last + 1)
    val current: StitcherToken get() = checkNotNull(iterator.peek()) { STREAM_END }
    val currentOrNull: StitcherToken? get() = iterator.peek()

    fun consume() = iterator.next()
    fun hasNext() = iterator.hasNext()

    companion object {
        fun LookaheadParser.placeholder(at: Int = index) = PlaceholderToken(source.source, at)
        fun LookaheadParser.placeholder(id: ProblemID, at: Int = index) = placeholder(at).also { problems.report(id, at) }

        fun LookaheadParser.consumeOrNull() = if (hasNext()) consume() else null
        fun LookaheadParser.consumeOrPlaceholder(): StitcherToken = consumeOrNull() ?: placeholder()

        inline fun LookaheadParser.consumeWhile(condition: (StitcherToken) -> Boolean): List<StitcherToken> = buildList {
            while (hasNext() && condition(current)) consuming(::add)
        }

        inline fun <T> LookaheadParser.consuming(action: (StitcherToken) -> T): T =
            action(current).also { iterator.next() }

        inline fun <T> LookaheadParser.advancing(action: (StitcherToken) -> T): T =
            current.let { iterator.next(); action(it) }
    }
}