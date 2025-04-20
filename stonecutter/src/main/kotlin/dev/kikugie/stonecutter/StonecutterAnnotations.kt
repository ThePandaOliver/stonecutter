package dev.kikugie.stonecutter

import kotlin.annotation.AnnotationTarget.*

/**
 * Marks members intended to be used as a part of the standard
 * Stonecutter configuration.
 */
@MustBeDocumented
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY, FIELD, TYPEALIAS)
public annotation class StonecutterAPI

/**
 * Marks members used for advanced configuration by
 * Stonecutter addons and case-specific fixes
 * and should be used with care.
 */
@MustBeDocumented
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY, FIELD, TYPEALIAS)
public annotation class StonecutterDevAPI

/**
 * Marks members that should not be used,
 * but need to have public visibility for technical reasons.
 */
@RequiresOptIn("This API is internal to Stonecutter and should not be used.", RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY, FIELD, TYPEALIAS)
public annotation class StonecutterInternalAPI

/**
 * Used for inserting external wiki links into KDoc comments at build time.
 * See `stonecutter/build.gradle.kts` for link definitions.
 * @see <a href="https://github.com/stonecutter-versioning/kdoclink">KSP plugin page</a>
 */
@Repeatable
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(CLASS, FUNCTION, PROPERTY, TYPEALIAS)
internal annotation class SCDocumentation(vararg val ids: String)

@DslMarker
@Retention(AnnotationRetention.BINARY) @Target(CLASS)
internal annotation class SCConfiguration

@DslMarker
@Retention(AnnotationRetention.BINARY) @Target(CLASS)
internal annotation class SCUtility

@DslMarker
@Retention(AnnotationRetention.BINARY) @Target(CLASS)
internal annotation class SCFilterSpec

@DslMarker
@Retention(AnnotationRetention.BINARY) @Target(CLASS)
internal annotation class SCReplacementSpec

@DslMarker
@Retention(AnnotationRetention.BINARY) @Target(CLASS)
internal annotation class SCFlagSpec