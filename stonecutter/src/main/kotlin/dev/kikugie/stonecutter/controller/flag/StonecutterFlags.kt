@file:JvmName("StonecutterFlags")
package dev.kikugie.stonecutter.controller.flag

public data class StonecutterFlag<T> (
    public val key: String,
    public val default: T
)

/**
 * Configures versioned source generation on IntelliJ sync.
 * When disabled, sourced will only be updated when the project is built.
 *
 * **Default**: `true`
 */
public val GENERATE_SOURCES_ON_SYNC: StonecutterFlag<Boolean> = StonecutterFlag("generateSourcesOnSync", true)

/**
 * Configures run configuration generation in IntelliJ.
 * When enabled, version switch tasks will be available per project in the
 * "Run configurations" dropdown menu.
 *
 * **Default**: `true`
 */
public val GENERATE_SWITCH_ACTIONS: StonecutterFlag<Boolean> = StonecutterFlag("generateSwitchActions", true)

/**
 * Configures the source generation mode for [dev.kikugie.stonecutter.build.StonecutterBuildExtension].
 * Normally, the build plugin won't detect source directories added after the source set creation.
 * With this flag enabled, it will do a second pass after project evaluation.
 * Disable if it causes issues and append source sets manually with the help of [dev.kikugie.stonecutter.build.task.StonecutterBuildTasks].
 *
 * **Default**: `true`
 */
public val APPEND_SOURCES_AFTER_EVAL: StonecutterFlag<Boolean> = StonecutterFlag("appendSourcesAfterEval", true)

/**
 * Configures the implicit receiver target used in file processing.
 * For more information on this functionality refer to the wiki.
 *
 * **Default**: `"minecraft"`
 */
public val IMPLICIT_RECEIVER: StonecutterFlag<String> = StonecutterFlag("implicitReceiver", "minecraft")
