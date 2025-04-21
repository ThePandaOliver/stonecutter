package dev.kikugie.semver.match

import dev.kikugie.semver.util.bind
import dev.kikugie.semver.util.countIn
import dev.kikugie.semver.util.then

fun interface VersionMatcher {
    /**
     * Matches a boundary for the parsed version in *[start]..<[end]*.
     * At this stage the version is not guaranteed to be valid,
     * only the potentially valid range is captured.
     *
     * @return The exclusive end of the matched sequence
     *         or 0 if nothing could be matched.
     * @throws IndexOutOfBoundsException If *[start]..<[end]* doesn't fit in the [source].
     */
    fun match(source: CharSequence, start: Int, end: Int): Int
}

object SemanticVersionMatcher : VersionMatcher {
    override fun match(source: CharSequence, start: Int, end: Int): Int =
        bind(start, end, source) then source.countIn(start, end, ::canBeSemver) + start

    private fun canBeSemver(char: Char) = when (char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', '+', '-', '.' -> true
        else -> false
    }
}

object PlainVersionMatcher : VersionMatcher {
    override fun match(source: CharSequence, start: Int, end: Int): Int =
        bind(start, end, source) then if (!canBeFirst(source[start])) start else source.countIn(start + 1, end, ::canBePart) + start + 1

    private fun canBeFirst(char: Char) = when (char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', '_' -> true
        else -> false
    }

    private fun canBePart(char: Char) = when (char) {
        '-', '+', '.' -> true
        else -> canBeFirst(char)
    }
}