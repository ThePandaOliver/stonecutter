package dev.kikugie.stonecutter.build.dsl

//import dev.kikugie.semver.data.Version
//import dev.kikugie.semver.data.VersionPredicate
//import dev.kikugie.stonecutter.SCUtility
//
//@SCUtility
//public interface VersionProvider<T : Version> {
//    public fun parseVersion(value: CharSequence): Result<T>
//    public fun parsePredicate(value: CharSequence): Result<VersionPredicate>
//
//    public fun eval(target: Version, vararg predicates: CharSequence): Boolean
//    public fun eval(target: CharSequence, vararg predicates: CharSequence): Boolean =
//        eval(parseVersion(target).getOrThrow(), *predicates)
//
//    public fun compare(left: CharSequence, right: CharSequence): Int =
//        parseVersion(left).getOrThrow() compareTo parseVersion(right).getOrThrow()
//}