package dev.kikugie.semver.data

import dev.kikugie.semver.util.allowedInStringVer
import dev.kikugie.semver.util.countIn
import dev.kikugie.semver.util.getOrDefault

private fun CharSequence.getOperator(start: Int) = when (this[start]) {
    '=' -> VersionOperator.EQUAL
    '^' -> VersionOperator.SAME_MAJOR
    '~' -> VersionOperator.SAME_MINOR
    '>' -> if (getOrDefault(start + 1) == '=') VersionOperator.GREATER_EQUAL else VersionOperator.GREATER
    '<' -> if (getOrDefault(start + 1) == '=') VersionOperator.LESS_EQUAL else VersionOperator.LESS
    else -> VersionOperator.IMPLICIT_EQUAL
}

data class VersionPredicate(
    val operator: VersionOperator,
    val version: Version
) {
    sealed interface Operations {
        fun locate(input: CharSequence, start: Int = 0, end: Int = input.length): Int
        fun parse(value: String): Result<VersionPredicate>
    }

    companion object : Operations {
        override fun locate(input: CharSequence, start: Int, end: Int): Int {
            Semantic.locate(input, start, end).let { if (it >= 0) return it }
            return Plain.locate(input, start, end)
        }

        override fun parse(value: String): Result<VersionPredicate> {
            Semantic.parse(value).let { if (it.isSuccess) return it }
            return Plain.parse(value)
        }

    }

    object Plain : Operations {
        override fun locate(input: CharSequence, start: Int, end: Int): Int {
            val coercedStart = start.coerceAtLeast(0)
            val coercedEnd = end.coerceAtMost(input.length)
            if (coercedStart >= coercedEnd) return -1

            var cursor = coercedStart
            cursor += when(val operator = input.getOperator(cursor)) {
                VersionOperator.SAME_MAJOR,
                VersionOperator.SAME_MINOR -> return -1
                VersionOperator.IMPLICIT_EQUAL -> 0
                else -> operator.literal.length.let {
                    it + input.countIn(cursor + it, coercedEnd, Char::isWhitespace)
                }
            }

            if (cursor >= coercedEnd) return -1
            cursor = StringVersion.locate(input, cursor, coercedEnd).also {
                if (it < 0) return -1
            }
            return cursor
        }

        override fun parse(value: String): Result<VersionPredicate> = kotlin.runCatching {
            require(value.isNotEmpty()) { "Predicate string cannot be empty" }
            val operator = value.getOperator(0)
            val offset = when(operator) {
                VersionOperator.SAME_MAJOR,
                VersionOperator.SAME_MINOR -> throw IllegalArgumentException("Operator '${operator.literal}' can't be used on a string version")
                VersionOperator.IMPLICIT_EQUAL -> when {
                    value[0].allowedInStringVer() -> 0
                    value[0].isWhitespace() -> throw IllegalArgumentException("Predicate string must be trimmed")
                    else -> throw IllegalArgumentException("Invalid predicate operator '${value[0]}'")
                }
                else -> operator.literal.length.let {
                    it + value.countIn(it, predicate = Char::isWhitespace)
                }
            }
            val version = StringVersion.parse(value.substring(offset)).getOrThrow()
            VersionPredicate(operator, version)
        }
    }

    object Semantic : Operations {
        override fun locate(input: CharSequence, start: Int, end: Int): Int {
            val coercedStart = start.coerceAtLeast(0)
            val coercedEnd = end.coerceAtMost(input.length)
            if (coercedStart >= coercedEnd) return -1

            var cursor = coercedStart
            cursor += when(val operator = input.getOperator(cursor)) {
                VersionOperator.IMPLICIT_EQUAL -> 0
                else -> operator.literal.length.let {
                    it + input.countIn(cursor + it, coercedEnd, Char::isWhitespace)
                }
            }

            if (cursor >= coercedEnd) return -1
            cursor = SemanticVersion.locate(input, cursor, coercedEnd).also {
                if (it < 0) return -1
            }
            return cursor
        }

        override fun parse(value: String): Result<VersionPredicate> = kotlin.runCatching {
            require(value.isNotEmpty()) { "Predicate string cannot be empty" }
            val operator = value.getOperator(0)
            val offset = when(operator) {
                VersionOperator.IMPLICIT_EQUAL -> when {
                    value[0].allowedInStringVer() -> 0
                    value[0].isWhitespace() -> throw IllegalArgumentException("Predicate string must be trimmed")
                    else -> throw IllegalArgumentException("Invalid predicate operator '${value[0]}'")
                }
                else -> operator.literal.length.let {
                    it + value.countIn(it, predicate =  Char::isWhitespace)
                }
            }
            val version = SemanticVersion.parse(value.substring(offset)).getOrThrow()
            VersionPredicate(operator, version)
        }
    }

    operator fun invoke(target: Version): Boolean =
        operator(target, version)
}
