package dev.kikugie.stonecutter

import kotlin.annotation.AnnotationTarget.*

/**
 * Annotated members are stable parts of the API available in configuration scripts.
 * Some public members may not be annotated with this in case the functionality
 * only needs to be public for addons, but not the end user.
 */
@MustBeDocumented
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY, FIELD, TYPEALIAS)
public annotation class StonecutterAPI

@MustBeDocumented
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY, FIELD, TYPEALIAS)
public annotation class StonecutterDevAPI

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