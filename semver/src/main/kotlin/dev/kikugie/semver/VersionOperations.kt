package dev.kikugie.semver

import dev.kikugie.semver.data.ImplicitEqualOperator
import dev.kikugie.semver.data.PlainVersion
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionOperator
import dev.kikugie.semver.match.CommonOperatorMatcher
import dev.kikugie.semver.match.CompositeOperatorMatcher
import dev.kikugie.semver.match.PlainVersionMatcher
import dev.kikugie.semver.match.SemanticOperatorMatcher
import dev.kikugie.semver.match.SemanticVersionMatcher
import dev.kikugie.semver.parsing.PlainVersionParser
import dev.kikugie.semver.parsing.SemanticVersionParser

interface VersionOperations {
    fun getVersionBoundaries(source: CharSequence, start: Int = 0, end: Int = source.length): IntRange
    fun getPredicateOperator(source: CharSequence, start: Int = 0, end: Int = source.length): VersionOperator
    fun parseFullVersion(input: CharSequence): Result<Version>
}

object SemanticVersionOperations : VersionOperations {
    override fun getVersionBoundaries(source: CharSequence, start: Int, end: Int): IntRange =
        start..<SemanticVersionMatcher.match(source, start, end)

    override fun getPredicateOperator(source: CharSequence, start: Int, end: Int): VersionOperator =
        CompositeOperatorMatcher(SemanticOperatorMatcher, CommonOperatorMatcher).match(source, start, end) ?: ImplicitEqualOperator

    override fun parseFullVersion(input: CharSequence): Result<SemanticVersion> =
        SemanticVersionParser.parseFullVersion(input)
}

object PlainVersionOperations : VersionOperations {
    override fun getVersionBoundaries(source: CharSequence, start: Int, end: Int): IntRange =
        start..PlainVersionMatcher.match(source, start, end)

    override fun getPredicateOperator(source: CharSequence, start: Int, end: Int): VersionOperator =
        CommonOperatorMatcher.match(source, start, end) ?: ImplicitEqualOperator

    override fun parseFullVersion(input: CharSequence): Result<PlainVersion> =
        PlainVersionParser.parseFullVersion(input)
}

object LenientVersionOperations : VersionOperations by SemanticVersionOperations {
    override fun getVersionBoundaries(source: CharSequence, start: Int, end: Int): IntRange =
        SemanticVersionOperations.getVersionBoundaries(source, start, end)
            .let { if (it.isEmpty()) PlainVersionOperations.getVersionBoundaries(source, start, end) else it }

    override fun parseFullVersion(input: CharSequence): Result<Version> =
        SemanticVersionOperations.parseFullVersion(input)
            .let { if (it.isFailure) PlainVersionOperations.parseFullVersion(input) else it }
}