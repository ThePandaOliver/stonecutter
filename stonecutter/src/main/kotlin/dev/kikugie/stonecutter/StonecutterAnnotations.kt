package dev.kikugie.stonecutter

/**
 * Annotated members are stable parts of the API available in configuration scripts.
 * Some public members may not be annotated with this in case the functionality
 * only needs to be public for addons, but not the end user.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class StonecutterAPI

/**
 * Annotated members are still treated as public API, but may cause issues when misused.
 */
@RequiresOptIn("This functionality is prone to cause issues when used incorrectly. Proceed with caution.")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class StonecutterDelicate

/**
 * Used for inserting external wiki links into KDoc comments at build time.
 * See `stonecutter/build.gradle.kts` for link definitions.
 * @see <a href="https://github.com/stonecutter-versioning/kdoclink">KSP plugin page</a>
 */
@Repeatable
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
internal annotation class SCDocumentation(vararg val ids: String)