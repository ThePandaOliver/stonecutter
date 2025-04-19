package dev.kikugie.semver.match

import dev.kikugie.semver.data.CommonOperator
import dev.kikugie.semver.data.SemanticOperator
import dev.kikugie.semver.data.VersionOperator
import dev.kikugie.semver.util.bind
import dev.kikugie.semver.util.then

interface OperatorMatcher {
    fun match(source: CharSequence, start: Int, end: Int): VersionOperator?
}

@JvmInline
value class CompositeOperatorMatcher(private val matchers: Iterable<OperatorMatcher>) : OperatorMatcher {
    constructor(vararg matchers: OperatorMatcher) : this(matchers.asIterable())
    override fun match(source: CharSequence, start: Int, end: Int): VersionOperator? =
        matchers.firstNotNullOfOrNull { it.match(source, start, end) }
}

object CommonOperatorMatcher : OperatorMatcher {
    override fun match(source: CharSequence, start: Int, end: Int): VersionOperator? = bind(start, end, source) then when (source[start]) {
        '=' -> CommonOperator.EQUAL
        '>' ->
            if (source.isNext('=', start, end)) CommonOperator.GREATER_EQUAL
            else CommonOperator.GREATER
        '<' ->
            if (source.isNext('=', start, end)) CommonOperator.LESS_EQUAL
            else CommonOperator.LESS
        else -> null
    }

    private fun CharSequence.isNext(char: Char, current: Int, end: Int) =
        current + 1 < end && this[current + 1] == char
}

object SemanticOperatorMatcher : OperatorMatcher {
    override fun match(source: CharSequence, start: Int, end: Int): VersionOperator? = bind(start, end, source) then when (source[start]) {
        '~' -> SemanticOperator.SAME_MINOR
        '^' -> SemanticOperator.SAME_MAJOR
        else -> null
    }
}