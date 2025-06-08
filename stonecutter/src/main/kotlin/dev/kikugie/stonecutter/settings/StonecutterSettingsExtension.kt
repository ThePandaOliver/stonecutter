package dev.kikugie.stonecutter.settings

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stonecutter.ProjectReference
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.TreeBuilder
import dev.kikugie.stonecutter.data.tree.TreeSettings
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import java.io.File
import dev.kikugie.semver.data.Version as ParsedVersion

private val LENIENT_JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

@OptIn(ExperimentalSerializationApi::class)
private fun readTreeSettings(file: File): Action<TreeBuilder> {
    require(file.extension.let { it == "json" || it == "json5" })
    { "Version setup file must be in JSON or JSON5 format. See Stonecutter wiki for more information." }
    val data: TreeSettings = file.inputStream().use { LENIENT_JSON.decodeFromStream(it) }
    return Action { applyData(data) }
}

@StonecutterAPI
public abstract class StonecutterSettingsExtension(internal val objects: ObjectFactory) : VersionOperations<ParsedVersion> {
    private lateinit var shared: Action<TreeBuilder>

    /**
     * Enables Kotlin buildscripts for the controller.
     * - `stonecutter.gradle` -> `stonecutter.gradle.kts`
     */
    public abstract val kotlinController: Property<Boolean>

    /**Buildscript used by all subprojects. Defaults to `build.gradle`.*/
    public abstract val centralScript: Property<String>

    /**Provides [VersionOperations], which work strictly with [SemanticVersion]s.*/
    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations

    /* Shared configuration */
    /**Stores the provided configuration to be used in [create] methods.*/
    public fun shared(action: Action<TreeBuilder>) {
        shared = action
    }

    /* File configuration */
    /**
     * Configures the specified [project] to be versioned with setup provided by [file].
     * @see ProjectReference
     */
    public fun create(project: ProjectReference, file: File): Unit =
        create(listOf(project), file)

    /**
     * Configures the specified [projects] to be versioned with setup provided by [file].
     * @see ProjectReference
     */
    public fun create(vararg projects: ProjectReference, file: File): Unit =
        create(projects.asIterable(), file)

    /**
     * Configures the specified [projects] to be versioned with setup provided by [file].
     * @see ProjectReference
     */
    public fun create(projects: Iterable<ProjectReference>, file: File): Unit =
        create(projects, readTreeSettings(file))

    /* Action configuration */
    /**
     * Configures the specified [project] to be versioned with setup provided by [action] or [shared].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(project: ProjectReference, action: Action<TreeBuilder> = shared): Unit =
        create(listOf(project), action)

    /**
     * Configures the specified [projects] to be versioned with setup provided by [action] or [shared].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(vararg projects: ProjectReference, action: Action<TreeBuilder> = shared): Unit =
        create(projects.asIterable(), action)

    /**
     * Configures the specified [projects] to be versioned with setup provided by [action] or [shared].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(projects: Iterable<ProjectReference>, action: Action<TreeBuilder> = shared): Unit =
        projects.forEach { create(it, objects.newInstance<TreeBuilder>(this).also(action::execute)) }

    /* Base configuration */
    protected abstract fun create(ref: ProjectReference, setup: TreeBuilder)
}