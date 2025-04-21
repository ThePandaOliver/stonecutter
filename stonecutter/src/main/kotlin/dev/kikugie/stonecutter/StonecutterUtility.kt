package dev.kikugie.stonecutter

import dev.kikugie.semver.VersionParser
import dev.kikugie.semver.VersionParsingException
import org.jetbrains.annotations.Contract

/**
 * Provides a set of pure functions to ease Stonecutter configurations.
 * Available in [StonecutterBuild] and [StonecutterController].
 */
@SCDocumentation("utility")
public interface StonecutterUtility {
    /**
     * Evaluates the passed version as [SemanticVersion] and compares to the given predicate(s),
     * which have to be separated by a space.
     * If the passed version is invalid [VersionParsingException] will be thrown
     *
     * @sample stonecutter_samples.eval.strict
     * @throws VersionParsingException
     * @see VersionParser.parsePredicate
     */
    @SCDocumentation("utility")
    @Contract(pure = true) @StonecutterAPI
    public fun eval(version: Version, predicate: String): Boolean {
        val target = VersionParser.parse(version).value
        return predicate.split(' ').all {
            VersionParser.parsePredicateLenient(it).value.eval(target)
        }
    }

    /**
     * Evaluates the passed version as [SemanticVersion] or [AnyVersion] and compares to the given predicate(s).
     *
     * @sample stonecutter_samples.eval.lenient
     * @see VersionParser.parsePredicateLenient
     */
    @SCDocumentation("utility")
    @Contract(pure = true) @StonecutterAPI
    public fun evalLenient(version: Version, predicate: String): Boolean {
        val target = VersionParser.parseLenient(version).value
        return predicate.split(' ').all {
            VersionParser.parsePredicateLenient(it).value.eval(target)
        }
    }

    /**
     * Parses both parameters as [SemanticVersion] and compares them.
     *
     * @return 1 if the [left] is greater, -1 if the [right] is greater, 0 if they are equal
     */
    @SCDocumentation("utility")
    @Contract(pure = true) @StonecutterAPI
    public fun compare(left: Version, right: Version): Int =
        VersionParser.parse(left).value.compareTo(VersionParser.parse(right).value)

    /**
     * Parses both parameters as [SemanticVersion] or [AnyVersion] and compares them.
     *
     * @return 1 if the [left] is greater, -1 if the [right] is greater, 0 if they are equal
     */
    @SCDocumentation("utility")
    @Contract(pure = true) @StonecutterAPI
    public fun compareLenient(left: Version, right: Version): Int =
        VersionParser.parseLenient(left).value.compareTo(VersionParser.parse(right).value)
}