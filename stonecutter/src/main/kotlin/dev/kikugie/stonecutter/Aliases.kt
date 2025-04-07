package dev.kikugie.stonecutter

import dev.kikugie.semver.SemanticVersion
import dev.kikugie.semver.StringVersion
import dev.kikugie.semver.VersionParser
import dev.kikugie.semver.VersionParsingException
import dev.kikugie.stitcher.lexer.TokenMatcher.Companion.isValidIdentifier

/**
 * Stonecutter version that may be either a [SemanticVersion] or a [StringVersion].
 * All [StringVersion]s are also valid [Identifier]s.
 *
 * @see [VersionParser.parseSemanticVersion]
 * @see [VersionParser.parseStringVersion]
 * @see [TokenMatcher.isValidIdentifier]
 */
public typealias AnyVersion = String

/**
 * Stonecutter version that must be a valid string representation of a [SemanticVersion].
 *
 * Parameters of this type are usually checked and will
 * throw [IllegalArgumentException] if they don't pass.
 *
 * @see [VersionParser.parseSemanticVersion]
 */
public typealias SemanticVersion = String
internal fun dev.kikugie.stonecutter.SemanticVersion.validateVersion() = try {
    VersionParser.parse(this, full = true).value
} catch (e: VersionParsingException) {
    throw IllegalArgumentException("Invalid semantic version: $this").apply {
        initCause(e)
    }
}

/**
 * Path in the Gradle project hierarchy.
 *
 * Root project can either be an empty string, or `:`.
 * Subprojects must be valid [Identifier]s, separated by `:`,
 * optionally having `:` at the start.
 */
public typealias ProjectPath = String