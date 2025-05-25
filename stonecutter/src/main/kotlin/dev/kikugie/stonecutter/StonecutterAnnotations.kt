package dev.kikugie.stonecutter

import kotlin.annotation.AnnotationTarget.*

/**
 * Marks members intended to be used as a part of the standard
 * Stonecutter configuration.
 */
@MustBeDocumented
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Target(CLASS, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
public annotation class StonecutterAPI

/**
 * Marks members that should not be used,
 * but need to have public visibility for technical reasons.
 */
@RequiresOptIn("This API is internal to Stonecutter and should not be used.", RequiresOptIn.Level.WARNING)
@Target(CLASS, TYPEALIAS, FUNCTION, PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class StonecutterInternalAPI