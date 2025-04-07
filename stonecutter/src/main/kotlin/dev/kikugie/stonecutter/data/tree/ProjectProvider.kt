package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.StonecutterProject

/**
 * Methods available in [dev.kikugie.stonecutter.settings.SettingsAbstraction.shared], [dev.kikugie.stonecutter.settings.SettingsAbstraction.create] and [dev.kikugie.stonecutter.data.tree.TreeBuilder.branch].
 * Extracted to an interface to ease configuration.
 */
public abstract class ProjectProvider {
    /**
     * Registers a [dev.kikugie.stonecutter.data.StonecutterProject] with separate project directory and version.
     * @sample stonecutter_samples.settings.single
     */
    @StonecutterAPI
    public fun vers(name: Identifier, version: AnyVersion): NodeProvider =
        versions(listOf(StonecutterProject.create(name, version)))

    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with the same directory and target versions.
     * @sample stonecutter_samples.settings.basic_vararg
     */
    @StonecutterAPI
    public fun versions(vararg versions: AnyVersion): NodeProvider =
        versions(versions.map { StonecutterProject.create(it, it) })


    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with the same directory and target versions.
     * @sample stonecutter_samples.settings.basic_iterable
     */
    @StonecutterAPI
    public fun versions(versions: Iterable<AnyVersion>): NodeProvider =
        versions(versions.map { StonecutterProject.create(it, it) })

    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with separate directory and target versions.
     * @sample stonecutter_samples.settings.pairs_vararg
     * @return [BNAN]
     */
    @StonecutterAPI @JvmName("versionsPairs")
    public fun versions(vararg versions: Pair<Identifier, AnyVersion>): NodeProvider =
        versions(versions.map { StonecutterProject.create(it.first, it.second) })

    /**
     * Registers multiple [dev.kikugie.stonecutter.data.StonecutterProject]s with separate directory and target versions.
     * @sample stonecutter_samples.settings.pairs_iterable
     * @return [BNAN]
     */
    @StonecutterAPI @JvmName("versionsPairs")
    public fun versions(versions: Iterable<Pair<Identifier, AnyVersion>>): NodeProvider =
        versions(versions.map { StonecutterProject.create(it.first, it.second) })

    internal abstract fun versions(versions: Iterable<StonecutterProject>): NodeProvider
}