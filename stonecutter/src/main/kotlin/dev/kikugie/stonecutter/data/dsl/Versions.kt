package dev.kikugie.stonecutter.data.dsl

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.semver.data.Version as ParsedVersion

/**
 * Provides version evaluation operations in
 * [StonecutterSettings][dev.kikugie.stonecutter.settings.StonecutterSettingsExtension],
 * [StonecutterController][dev.kikugie.stonecutter.controller.StonecutterControllerExtension] and
 * [StonecutterBuild][dev.kikugie.stonecutter.build.StonecutterBuildExtension].
 *
 * These implementations provide lenient parsing, which first attempts to parse the version as a
 * [SemanticVersion][dev.kikugie.semver.data.SemanticVersion] and then falls back to a
 * [StringVersion][dev.kikugie.semver.data.StringVersion].
 *
 * @see dev.kikugie.semver.data.StringVersion
 * @see dev.kikugie.semver.data.SemanticVersion
 */
@StonecutterAPI
public interface VersionOperations<T : ParsedVersion> {
    /**
     * Checks if the provided [version] can be parsed.
     */
    public fun check(version: String): Boolean
    /**
     * Parses the provided [version] string into the required type [T].
     * @throws dev.kikugie.semver.impl.VersionParsingException if the provided [version] cannot be parsed into the required type.
     */
    public fun parse(version: String): T

    /**
     * Parses the provided [version] string into the required type [T] and applies the provided [predicates] to it.
     * Each predicate string may contain multiple predicates separated by spaces.
     * All predicates must succeed for the version to be considered valid.
     * @throws dev.kikugie.semver.impl.VersionParsingException if the provided [version] or any of [predicates] cannot be parsed into the required type.
     */
    public fun eval(version: String, vararg predicates: String): Boolean =
        eval(parse(version), *predicates)

    /**
     * Applies the provided [predicates] to the provided [version] and returns whether they all succeed.
     * Each predicate string may contain multiple predicates separated by spaces.
     * All predicates must succeed for the version to be considered valid.
     * @throws dev.kikugie.semver.impl.VersionParsingException if any of the provided [predicates] cannot be parsed into the required type.
     */
    public fun eval(version: ParsedVersion, vararg predicates: String): Boolean

    /**
     * Compares two version strings according to the [Comparable][java.lang.Comparable] specification.
     * @throws dev.kikugie.semver.impl.VersionParsingException if either of the provided [version] strings cannot be parsed into the required type.
     */
    public fun compare(version: String, other: String): Int =
        parse(version) compareTo parse(other)
}