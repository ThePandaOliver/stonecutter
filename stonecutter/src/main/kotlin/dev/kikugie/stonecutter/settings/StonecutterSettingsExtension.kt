package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.LENIENT_JSON
import dev.kikugie.stonecutter.SCDocumentation
import dev.kikugie.stonecutter.data.tree.TreeBuilder
import dev.kikugie.stonecutter.data.tree.TreeSettings
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.Action
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance
import java.io.File

@SCDocumentation("settings")
public abstract class StonecutterSettingsExtension {
    private lateinit var shared: Action<TreeBuilder>

    /**
     * Enables Kotlin buildscripts for the controller.
     * - `stonecutter.gradle` -> `stonecutter.gradle.kts`
     */
    @StonecutterAPI
    public abstract val kotlinController: Property<Boolean>

    /**Buildscript used by all subprojects. Defaults to `build.gradle`.*/
    @StonecutterAPI
    public abstract val centralScript: Property<String>

    /* Shared configuration */
    /**Stores the provided configuration to be used in [create] methods.*/
    @StonecutterAPI
    public fun shared(action: Action<TreeBuilder>) {
        shared = action
    }

    /* File configuration */
    /**Configures the specified [project] to be versioned with setup provided by [file].*/
    @SCDocumentation("settings.json")
    @StonecutterAPI
    public fun create(project: ProjectPath, file: File): String =
        create(descriptor(project), file).let { BNAN }

    /**Configures the specified [project] to be versioned with setup provided by [file].*/
    @SCDocumentation("settings.json")
    @StonecutterAPI
    public fun create(project: ProjectDescriptor, file: File): Unit =
        create(listOf(project), file)

    /**Configures the specified [projects] to be versioned with setup provided by [file].*/
    @SCDocumentation("settings.json")
    @StonecutterAPI
    public fun create(vararg projects: ProjectPath, file: File): String =
        create(projects.map(::descriptor), file).let { BNAN }

    /**Configures the specified [projects] to be versioned with setup provided by [file].*/
    @SCDocumentation("settings.json")
    @StonecutterAPI
    public fun create(vararg projects: ProjectDescriptor, file: File): Unit =
        create(projects.toList(), file)

    /**Configures the specified [projects] to be versioned with setup provided by [file].*/
    @SCDocumentation("settings.json")
    @StonecutterAPI
    public fun create(projects: Iterable<ProjectPath>, file: File): String =
        create(projects.map(::descriptor), file).let { BNAN }

    /**Configures the specified [projects] to be versioned with setup provided by [file].*/
    @OptIn(ExperimentalSerializationApi::class)
    @SCDocumentation("settings.json")
    @StonecutterAPI
    public fun create(projects: Iterable<ProjectDescriptor>, file: File) {
        require(file.extension.let { it == "json" || it == "json5" }) { "Version setup file must be in JSON or JSON5 format. See Stonecutter wiki for more information." }
        val data: TreeSettings = file.inputStream().use { LENIENT_JSON.decodeFromStream(it) }
        create(projects, Action { applyData(data) })
    }

    /* Action configuration */
    /**Configures the specified [project] to be versioned with setup provided by [action] or [shared].*/
    @SCDocumentation("settings.create")
    @StonecutterAPI
    @JvmOverloads
    public fun create(project: ProjectPath, action: Action<TreeBuilder> = shared): String =
        create(descriptor(project), action).let { BNAN }

    /**Configures the specified [project] to be versioned with setup provided by [action] or [shared].*/
    @SCDocumentation("settings.create")
    @StonecutterAPI
    @JvmOverloads
    public fun create(project: ProjectDescriptor, action: Action<TreeBuilder> = shared): Unit =
        create(listOf(project), action)

    /**Configures the specified [projects] to be versioned with setup provided by [action] or [shared].*/
    @SCDocumentation("settings.create")
    @StonecutterAPI
    @JvmOverloads
    public fun create(vararg projects: ProjectPath, action: Action<TreeBuilder> = shared): String =
        create(projects.map(::descriptor), action).let { BNAN }

    /**Configures the specified [projects] to be versioned with setup provided by [action] or [shared].*/
    @SCDocumentation("settings.create")
    @StonecutterAPI
    @JvmOverloads
    public fun create(vararg projects: ProjectDescriptor, action: Action<TreeBuilder> = shared): Unit =
        create(projects.toList(), action)

    /**Configures the specified [projects] to be versioned with setup provided by [action] or [shared].*/
    @SCDocumentation("settings.create")
    @StonecutterAPI
    @JvmOverloads
    public fun create(projects: Iterable<ProjectPath>, action: Action<TreeBuilder> = shared): String =
        create(projects.map(::descriptor), action).let { BNAN }

    /**Configures the specified [projects] to be versioned with setup provided by [action] or [shared].*/
    @SCDocumentation("settings.create")
    @StonecutterAPI
    @JvmOverloads
    public fun create(projects: Iterable<ProjectDescriptor>, action: Action<TreeBuilder> = shared): Unit =
        projects.forEach { create(it, objects.newInstance<TreeBuilder>(this).also(action::execute)) }

    /* Base configuration */
    internal abstract val providers: ProviderFactory
    internal abstract val objects: ObjectFactory
    protected abstract fun create(project: ProjectDescriptor, setup: TreeBuilder)
    protected abstract fun descriptor(path: ProjectPath): ProjectDescriptor
}