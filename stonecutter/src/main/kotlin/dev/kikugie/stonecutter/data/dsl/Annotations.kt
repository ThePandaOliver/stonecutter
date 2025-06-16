package dev.kikugie.stonecutter.data.dsl

import org.gradle.api.provider.Provider

@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class StonecutterParametersDSL

@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class StonecutterReplacementSpec

/**
 * Marks a function that will realise values of one or more [Provider]s.
 * In some cases doing so may attempt to access data that is not available yet.
 * Therefore, the function should be used cautiously.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.SOURCE)
public annotation class EagerOperation