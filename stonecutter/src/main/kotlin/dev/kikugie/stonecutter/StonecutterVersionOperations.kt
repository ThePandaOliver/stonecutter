package dev.kikugie.stonecutter

import dev.kikugie.semver.LenientVersionOperations
import dev.kikugie.semver.SemanticVersionOperations
import dev.kikugie.semver.data.CommonOperator
import dev.kikugie.semver.data.PlainVersion
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.VersionPredicate
import dev.kikugie.semver.util.countIn
import dev.kikugie.semver.data.Version as LenientVersion

public interface StonecutterVersionOperations {
    public fun isSemanticVersion(value: CharSequence): Boolean = parseSemanticVersion(value).isSuccess
    public fun isLenientVersion(value: CharSequence): Boolean = parseLenientVersion(value).isSuccess

    public fun parseSemanticVersion(value: CharSequence): Result<SemanticVersion> = SemanticVersionOperations.parseFullVersion(value)
    public fun parseLenientVersion(value: CharSequence): Result<LenientVersion> = LenientVersionOperations.parseFullVersion(value)

    public fun parseSemanticPredicate(value: CharSequence): Result<VersionPredicate> = parseVersionPredicate(value)
    public fun parseLenientPredicate(value: CharSequence): Result<VersionPredicate> = parseVersionPredicate(value).mapCatching {
        if (it.version is PlainVersion)
            require(it.operator is CommonOperator) { "Semantic operator '${it.operator.literal}' is not supported for plain versions" }
        it
    }

    public fun eval(version: CharSequence, vararg predicates: CharSequence, lenient: Boolean = true): Boolean = when(lenient) {
        true -> evalLenient(version, *predicates)
        false -> evalSemantic(version, *predicates)
    }

    public fun evalSemantic(version: CharSequence, vararg predicates: CharSequence): Boolean {
        val target = parseSemanticVersion(version).getOrThrow()
        val flattened = predicates.flatMap(::getPredicateList)
        return flattened.all { it(target) }
    }

    public fun evalLenient(version: CharSequence, vararg predicates: CharSequence): Boolean {
        val target = parseLenientVersion(version).getOrThrow()
        val flattened = predicates.flatMap(::getPredicateList)
        return flattened.all { it(target) }
    }

    private fun parseVersionPredicate(value: CharSequence): Result<VersionPredicate> {
        require(value.isNotBlank()) { "Predicate cannot be blank" }
        val operator = SemanticVersionOperations.getPredicateOperator(value)
        val offset = operator.literal.length.let { it + value.countIn(it, predicate = Char::isWhitespace) }
        return SemanticVersionOperations.parseFullVersion(value.substring(offset)) // TODO: Print error nicely
            .map { VersionPredicate(operator, it) }
    }

    private fun getPredicateBoundary(value: CharSequence, offset: Int): IntRange {
        val operator = SemanticVersionOperations.getPredicateOperator(value)
        val offset = operator.literal.length.let { it + value.countIn(it, predicate = Char::isWhitespace) }
        return SemanticVersionOperations.getVersionBoundaries(value, offset)
    }

    private fun getPredicateList(value: CharSequence): List<VersionPredicate> = buildList {
        var offset = 0
        while (offset < value.length) when {
            value[offset].isWhitespace() -> offset++
            else -> {
                val predicate = value.substring(getPredicateBoundary(value, offset))
                this += parseVersionPredicate(predicate).getOrThrow() // TODO: Print error nicely
                offset += predicate.length
            }
        }
    }
}