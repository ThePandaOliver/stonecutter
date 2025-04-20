package dev.kikugie.semver.parsing

import dev.kikugie.semver.data.PlainVersion
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.match.PlainVersionMatcher

interface VersionParser<T : Version> {
    fun parseFullVersion(input: CharSequence): Result<T>
}

object SemanticVersionParser : VersionParser<SemanticVersion> {
    override fun parseFullVersion(input: CharSequence): Result<SemanticVersion> =
        parseImpl(input)
}

object PlainVersionParser : VersionParser<PlainVersion> {
    override fun parseFullVersion(input: CharSequence): Result<PlainVersion> = when(val end = PlainVersionMatcher.match(input, 0, input.length)) {
        input.length -> Result.success(PlainVersion(input.toString()))
        else -> Result.failure(VersionParsingException("Invalid character '${input[end]}' $input at $end", end..end))
    }
}
