package dev.kikugie.semver

import dev.kikugie.semver.data.*
import dev.kikugie.semver.match.*
import dev.kikugie.semver.parsing.PlainVersionParser
import dev.kikugie.semver.parsing.SemanticVersionParser
import dev.kikugie.semver.parsing.VersionParsingException
import dev.kikugie.semver.util.countIn
import dev.kikugie.semver.util.getOrDefault

interface VersionOperations {
    fun getVersionBoundary(source: CharSequence, start: Int = 0, end: Int = source.length): Int
    fun getPredicateBoundary(source: CharSequence, start: Int = 0, end: Int = source.length): Int =
        defaultGetPredicateBoundary(source, start, end)

    fun parseOperator(source: CharSequence, offset: Int = 0): VersionOperator?
    fun parseVersion(source: CharSequence): Result<Version>
    fun parsePredicate(source: CharSequence): Result<VersionPredicate> =
        defaultParsePredicate(source)

    private fun defaultGetPredicateBoundary(source: CharSequence, start: Int, end: Int): Int {
        var operator = parseOperator(source, start)
        if (operator == null && source.getOrDefault(start).isWhitespace()) return start
        else operator = operator ?: ImplicitEqualOperator
        var offset = operator.literal.length
        offset += source.countIn(offset, predicate = Char::isWhitespace)
        return getVersionBoundary(source, offset).let { if (it == offset) start else it }
    }

    private fun defaultParsePredicate(source: CharSequence): Result<VersionPredicate> {
        if (getPredicateBoundary(source) != source.length) return Result.failure(VersionParsingException("Invalid predicate", source.indices))
        val operator = parseOperator(source) ?: ImplicitEqualOperator
        return parseVersion(source).map { VersionPredicate(operator, it) }
    }
}

object SemanticVersionOperations : VersionOperations {
    override fun getVersionBoundary(source: CharSequence, start: Int, end: Int): Int =
        SemanticVersionMatcher.match(source, start, end)

    override fun getPredicateBoundary(source: CharSequence, start: Int, end: Int): Int = super.getPredicateBoundary(source, start, end)
        .let { if (parseOperator(source, start) is SemanticOperator) start else it }

    override fun parseOperator(source: CharSequence, offset: Int): VersionOperator? =
        CompositeOperatorMatcher(SemanticOperatorMatcher, CommonOperatorMatcher).match(source, offset, source.length)

    override fun parseVersion(source: CharSequence): Result<SemanticVersion> =
        SemanticVersionParser.parseFullVersion(source)

    override fun parsePredicate(source: CharSequence): Result<VersionPredicate> = super.parsePredicate(source).mapCatching {
        if (it.operator is SemanticOperator) throw VersionParsingException(
            "Plain version doesn't support semantic operators",
            it.operator.literal.indices
        )
        it
    }
}

object PlainVersionOperations : VersionOperations {
    override fun getVersionBoundary(source: CharSequence, start: Int, end: Int): Int =
        PlainVersionMatcher.match(source, start, end)

    override fun parseOperator(source: CharSequence, offset: Int): VersionOperator? =
        CommonOperatorMatcher.match(source, offset, source.length)

    override fun parseVersion(source: CharSequence): Result<PlainVersion> =
        PlainVersionParser.parseFullVersion(source)
}

object LenientVersionOperations : VersionOperations  {
    private val DELEGATES = listOf(SemanticVersionOperations, PlainVersionOperations)

    override fun getVersionBoundary(source: CharSequence, start: Int, end: Int): Int =
        DELEGATES.firstNotNullOfOrNull { it.getVersionBoundary(source, start, end).takeIf { it != start } } ?: start

    override fun getPredicateBoundary(source: CharSequence, start: Int, end: Int): Int =
        DELEGATES.firstNotNullOfOrNull { it.getPredicateBoundary(source, start, end).takeIf { it != start } } ?: start

    override fun parseOperator(source: CharSequence, offset: Int): VersionOperator? =
        DELEGATES.firstNotNullOfOrNull { it.parseOperator(source, offset) }

    override fun parseVersion(source: CharSequence): Result<Version> {
        var last: Result<Version>? = null
        for (delegate in DELEGATES) {
            last = delegate.parseVersion(source)
            if (last.isSuccess) return last
        }
        return last!!
    }

    override fun parsePredicate(source: CharSequence): Result<VersionPredicate> {
        var last: Result<VersionPredicate>? = null
        for (delegate in DELEGATES) {
            last = delegate.parsePredicate(source)
            if (last.isSuccess) return last
        }
        return last!!
    }
}