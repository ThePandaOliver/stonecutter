package dev.kikugie.stonecutter.build.dsl

import dev.kikugie.semver.LenientVersionOperations
import dev.kikugie.semver.PlainVersionOperations
import dev.kikugie.semver.SemanticVersionOperations
import dev.kikugie.semver.VersionOperations
import dev.kikugie.semver.data.PlainVersion
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionPredicate
import dev.kikugie.semver.parsing.VersionParsingException

private fun unpackPredicates(value: CharSequence, matcher: VersionOperations): List<VersionPredicate> = buildList {
    var offset = 0
    while (offset < value.length) when {
        value[offset].isWhitespace() -> offset++
        else -> {
            val boundary = matcher.getPredicateBoundary(value, offset)
            if (boundary == offset) throw VersionParsingException("Invalid predicate", offset..boundary)
            this += matcher.parsePredicate(value.subSequence(offset, boundary)).getOrThrow()
            offset = boundary
        }
    }
}

public interface VersionProvider<T : Version> {
    public fun parseVersion(value: CharSequence): Result<T>
    public fun parsePredicate(value: CharSequence): Result<VersionPredicate>

    public fun eval(target: Version, vararg predicates: CharSequence): Boolean
    public fun eval(target: CharSequence, vararg predicates: CharSequence): Boolean =
        eval(parseVersion(target).getOrThrow(), *predicates)

    public fun compare(left: CharSequence, right: CharSequence): Int =
        parseVersion(left).getOrThrow() compareTo parseVersion(right).getOrThrow()

    public companion object : VersionProvider<Version> {
        public val semantic: VersionProvider<SemanticVersion> = object : VersionProvider<SemanticVersion> {
            override fun parseVersion(value: CharSequence): Result<SemanticVersion> = SemanticVersionOperations.parseVersion(value)
            override fun parsePredicate(value: CharSequence): Result<VersionPredicate> = SemanticVersionOperations.parsePredicate(value)
            override fun eval(target: Version, vararg predicates: CharSequence): Boolean =
                predicates.flatMap { unpackPredicates(it, SemanticVersionOperations) }.all { it(target) }
        }

        public val plain: VersionProvider<PlainVersion> = object : VersionProvider<PlainVersion> {
            override fun parseVersion(value: CharSequence): Result<PlainVersion> = PlainVersionOperations.parseVersion(value)
            override fun parsePredicate(value: CharSequence): Result<VersionPredicate> = PlainVersionOperations.parsePredicate(value)
            override fun eval(target: Version, vararg predicates: CharSequence): Boolean =
                predicates.flatMap { unpackPredicates(it, PlainVersionOperations) }.all { it(target) }
        }

        override fun parseVersion(value: CharSequence): Result<Version> = LenientVersionOperations.parseVersion(value)
        override fun parsePredicate(value: CharSequence): Result<VersionPredicate> = LenientVersionOperations.parsePredicate(value)
        override fun eval(target: Version, vararg predicates: CharSequence): Boolean =
            predicates.flatMap { unpackPredicates(it, LenientVersionOperations) }.all { it(target) }
    }
}