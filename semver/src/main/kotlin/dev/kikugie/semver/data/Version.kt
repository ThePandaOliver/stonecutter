package dev.kikugie.semver.data

import dev.kikugie.semver.parse.SemanticParser
import dev.kikugie.semver.util.allowedInStringVer
import dev.kikugie.semver.util.countIn
import kotlinx.serialization.Serializable
import java.util.StringTokenizer

@Serializable
sealed interface Version : Comparable<Version> {
    sealed interface Operations {
        fun parse(value: String): Result<Version>
        fun locate(input: CharSequence, start: Int = 0, end: Int = input.length): Int
    }

    companion object : Operations {
        override fun parse(value: String): Result<Version> {
            SemanticVersion.parse(value).let { if (it.isSuccess) return it }
            return StringVersion.parse(value)
        }

        override fun locate(input: CharSequence, start: Int, end: Int): Int {
            SemanticVersion.locate(input, start, end).let { if (it >= 0) return it }
            return StringVersion.locate(input, start, end)
        }
    }
}

/**
 * Plain-string version representation, which is compared lexicographically.
 * When compared to a [SemanticVersion], [SemanticVersion.toString] value is used.
 *
 * The version value must only contain English alphanumeric characters, dashes, and underscores.
 * The default constructor **does not** validate the input - for this purpose use [StringVersion.parse].
 */
@Serializable @JvmInline
value class StringVersion(val value: String) : Version {
    companion object : Version.Operations {
        /**
         * Validates the input [value] as a [StringVersion], returning the corresponding [Result].
         */
        @JvmStatic override fun parse(value: String): Result<StringVersion> = kotlin.runCatching {
            require(value.isNotEmpty()) { "Version string cannot be empty" }
            require(value.all(Char::allowedInStringVer)) { "Version string contains invalid characters" }
            StringVersion(value)
        }

        /**
         * Loosely determines the [StringVersion] boundary in the given [input] in [start]..<[end].
         * Returns the exclusive end index of the version string, or -1 if the input is not a valid version string.
         */
        @JvmStatic override fun locate(input: CharSequence, start: Int, end: Int): Int {
            val coercedStart = start.coerceAtLeast(0)
            val coercedEnd = end.coerceAtMost(input.length)
            return if (coercedStart >= coercedEnd) -1
            else coercedStart + input.countIn(coercedStart, coercedEnd, Char::allowedInStringVer)
        }
    }

    override fun toString(): String = value
    override fun compareTo(other: Version): Int =
        value.compareTo(other.toString())
}

/**
 * Semantic version representation, which loosely follows the [SemVer](https://semver.org/) specification with a few exceptions:
 * - The version code can have any number of identifiers, allowing versions like `1.0.0.1`.
 *   When comparing versions with different number of core components, the rest is treated as 0:
 *   `1.2.3 < 1.2.3.1` is the same as `1.2.3.0 < 1.2.3.1`.
 * - The version core components can have any number of leading zeros:
 *   `000.1.0` is the same as `0.1.0`.
 * - Pre-release modifiers can contain dashes, dots, and underscores.
 *   When comparing versions, the one without a modifier is considered greater.
 *   The modifiers are split at dots, with each segment being compared numerically or lexicographically:
 *   `1.0-alpha < 1.0-alpha.1 < 1.0-beta < 1.0-beta.2 < 1.0-rc.1 < 1.0`.
 *  - Build metadata modifiers can contain dashes, dots, and underscores.
 *
 *  The default constructor **does not** validate the input - for this purpose use [SemanticVersion.parse].
 */
@Serializable
data class SemanticVersion(
    val components: IntArray,
    val preRelease: String = "",
    val buildMetadata: String = "",
) : Version {
    companion object : Version.Operations {
        /**
         * Parses the given [value] as a [SemanticVersion], returning the corresponding [Result].
         */
        @JvmStatic override fun parse(value: String): Result<SemanticVersion> =
            SemanticParser(value).parse()

        /**
         * Loosely determines the [SemanticVersion] boundary in the given [input] in [start]..<[end].
         * Returns the exclusive end index of the version string, or -1 if the input is not a valid semantic version string.
         */
        @JvmStatic override fun locate(input: CharSequence, start: Int, end: Int): Int {
            val coercedStart = start.coerceAtLeast(0)
            val coercedEnd = end.coerceAtMost(input.length)
            return if (coercedStart >= coercedEnd) -1
            else coercedStart + input.countIn(coercedStart, coercedEnd) {
                it.allowedInStringVer() || it == '.' || it == '+'
            }
        }
    }

    private val friendlyName by lazy {
        buildString {
            append(components.joinToString("."))
            if (preRelease.isNotEmpty())
                append("-$preRelease")
            if (buildMetadata.isNotEmpty())
                append("+$buildMetadata")
        }
    }

    override fun toString(): String = friendlyName
    override fun compareTo(other: Version): Int = when(other) {
        is StringVersion -> friendlyName.compareTo(other.value)
        is SemanticVersion -> compareComponents(other)
            .let { if (it != 0) it else compareToPreModifier(other) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticVersion) return false

        if (!components.contentEquals(other.components)) return false
        if (preRelease != other.preRelease) return false
        if (buildMetadata != other.buildMetadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = components.contentHashCode()
        result = 31 * result + preRelease.hashCode()
        result = 31 * result + buildMetadata.hashCode()
        return result
    }

    private fun compareComponents(other: SemanticVersion): Int {
        for (i in 0 until maxOf(components.size, other.components.size)) {
            val first = components.getOrElse(i) { 0 }
            val second = other.components.getOrElse(i) { 0 }
            first.compareTo(second).let { if (it != 0) return it }
        }
        return 0
    }

    private fun compareModifiers(a: String, b: String) = when {
        a.isEmpty() && b.isNotEmpty() -> -1
        a.isNotEmpty() && b.isEmpty() -> 1
        else -> 0
    }

    private fun compareToPreModifier(other: SemanticVersion): Int {
        val byPreRelease = compareModifiers(preRelease, other.preRelease)
        if (byPreRelease != 0 && preRelease.isNotEmpty()) return -byPreRelease

        val myTokenizer = StringTokenizer(preRelease, ".")
        val otherTokenizer = StringTokenizer(other.preRelease, ".")

        while (myTokenizer.hasMoreElements() || otherTokenizer.hasMoreElements()) {
            if (!myTokenizer.hasMoreElements()) return -1
            if (!otherTokenizer.hasMoreElements()) return 1

            val myPart = myTokenizer.nextToken()
            val otherPart = otherTokenizer.nextToken()

            val myPartInt = myPart.toIntOrNull()
            val otherPartInt = otherPart.toIntOrNull()

            if (myPartInt != null && otherPartInt != null) {
                val compare = myPartInt.compareTo(otherPartInt)
                if (compare != 0) return compare
            }
            if (myPartInt == null && otherPartInt != null)
                return 1
            if (myPartInt != null && otherPartInt == null)
                return -1
            val compare = myPart.compareTo(otherPart)
            if (compare != 0) return compare
        }
        return 0
    }
}