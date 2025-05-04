package dev.kikugie.stonecutter.data.dsl

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.semver.data.Version as ParsedVersion

@StonecutterAPI
public interface VersionOperations<T : ParsedVersion> {
    public fun parse(version: String): T

    public fun eval(version: String, vararg predicates: String): Boolean = eval(parse(version), *predicates)
    public fun eval(version: ParsedVersion, vararg predicates: String): Boolean

    public fun compare(version: String, other: String): Int =
        parse(version) compareTo parse(other)
}