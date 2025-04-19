package dev.kikugie.semver.data

import java.util.StringTokenizer
import kotlin.math.max

interface Version : Comparable<Version>

data class SemanticVersion(
    val components: IntArray,
    val preRelease: String = "",
    val buildMetadata: String = "",
) : Version {
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
        is SemanticVersion -> compareToComponents(other).takeIf { it != 0 }
            ?: compareToPreModifier(other)
        else -> friendlyName compareTo other.toString()
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

    private fun compareToComponents(other: SemanticVersion): Int {
        for (i in 0 until max(components.size, other.components.size)) {
            val first = components.getOrElse(i) { 0 }
            val second = other.components.getOrElse(i) { 0 }
            val compare = first.compareTo(second)
            if (compare != 0) return compare
        }
        return 0
    }

    private fun compareToPreModifier(other: SemanticVersion): Int {
        if (preRelease.isEmpty() && preRelease.isEmpty()) return 0
        if (preRelease.isEmpty() && other.preRelease.isNotEmpty()) return 1
        if (buildMetadata.isNotEmpty() && other.buildMetadata.isEmpty()) return -1

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

@JvmInline
value class PlainVersion(val value: String) : Version {
    override fun toString(): String = value
    override fun compareTo(other: Version): Int =
        value compareTo other.toString()
}