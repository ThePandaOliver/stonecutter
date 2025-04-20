package dev.kikugie.stonecutter

import dev.kikugie.stonecutter.build.dsl.VersionProvider
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
    @StonecutterAPI @SCDocumentation("utility") @Contract(pure = true) @Deprecated("Use checks DSL")
    public fun eval(version: Version, predicate: String): Boolean =
        VersionProvider.semantic.eval(version, predicate)

    /**
     * Evaluates the passed version as [SemanticVersion] or [Version] and compares to the given predicate(s).
     *
     * @sample stonecutter_samples.eval.lenient
     * @see VersionParser.parsePredicateLenient
     */
    @StonecutterAPI @SCDocumentation("utility") @Contract(pure = true) @Deprecated("Use checks DSL")
    public fun evalLenient(version: Version, predicate: String): Boolean =
        VersionProvider.eval(version, predicate)

    /**
     * Parses both parameters as [SemanticVersion] and compares them.
     *
     * @return 1 if the [left] is greater, -1 if the [right] is greater, 0 if they are equal
     */
    @StonecutterAPI @SCDocumentation("utility") @Contract(pure = true) @Deprecated("Use checks DSL")
    public fun compare(left: Version, right: Version): Int =
        VersionProvider.semantic.compare(left, right)

    /**
     * Parses both parameters as [SemanticVersion] or [Version] and compares them.
     *
     * @return 1 if the [left] is greater, -1 if the [right] is greater, 0 if they are equal
     */
    @StonecutterAPI @SCDocumentation("utility") @Contract(pure = true) @Deprecated("Use checks DSL")
    public fun compareLenient(left: Version, right: Version): Int =
        VersionProvider.compare(left, right)
}