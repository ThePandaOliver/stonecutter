package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.data.StonecutterProject

/**
 * Methods available in [dev.kikugie.stonecutter.settings.StonecutterSettingsExtension.shared],
 * [dev.kikugie.stonecutter.settings.StonecutterSettingsExtension.create] and [dev.kikugie.stonecutter.data.tree.TreeBuilder.branch].
 * Extracted to an interface to ease configuration.
 */
@OptIn(StonecutterInternalAPI::class)
public abstract class ProjectProvider {
    /**
     * Registers a [dev.kikugie.stonecutter.data.StonecutterProject] with separate project directory and version.
     */
    public fun vers(name: Identifier, version: Version): NodeProvider =
        versions(listOf(StonecutterProject(name, version)))

    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with the same directory and target versions.
     */
    public fun versions(vararg versions: Version): NodeProvider =
        versions(versions.map { StonecutterProject(it, it) })


    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with the same directory and target versions.
     */
    public fun versions(versions: Iterable<Version>): NodeProvider =
        versions(versions.map { StonecutterProject(it, it) })

    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with separate directory and target versions.
     */
    @JvmName("versionsPairs")
    public fun versions(vararg versions: Pair<Identifier, Version>): NodeProvider =
        versions(versions.map { StonecutterProject(it.first, it.second) })

    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with separate directory and target versions.
     */
    @JvmName("versionsPairs")
    public fun versions(versions: Iterable<Pair<Identifier, Version>>): NodeProvider =
        versions(versions.map { StonecutterProject(it.first, it.second) })

    internal abstract fun versions(versions: Iterable<StonecutterProject>): NodeProvider
}