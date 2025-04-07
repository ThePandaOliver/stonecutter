@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stonecutter.util

import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.keysToString
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.DefaultTaskExecutionRequest
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import org.gradle.work.InputChanges
import java.io.File

public typealias TaskProviderMap<T> = Map<String, TaskProvider<T>>
internal typealias MutableTaskProviderMap<T> = MutableMap<String, TaskProvider<T>>

internal val Project.sourceSets: SourceSetContainer
    get() = project.the<SourceSetContainer>()

internal inline operator fun <T> Provider<T>.invoke(): T = get()
internal inline operator fun <T> ListProperty<T>.invoke(): List<T> = get()
internal inline operator fun <K : Any, V> MapProperty<K, V>.invoke(): Map<K, V> = get()
internal inline operator fun <K : Any, V> MapProperty<K, V>.get(key: K): Provider<V> = getting(key)

internal inline operator fun <T> Property<T>.invoke(value: T?) = set(value)
internal inline operator fun <T> ListProperty<T>.invoke(elements: Iterable<T>?) = set(elements)
internal inline operator fun <T> ListProperty<T>.invoke(vararg elements: T) = set(elements.asIterable())
internal inline operator fun <K : Any, V> MapProperty<K, V>.invoke(map: Map<K, V>?) = set(map)
internal inline operator fun <K : Any, V> MapProperty<K, V>.invoke(pairs: Iterable<Pair<K, V>>) = set(pairs.toMap())
internal inline operator fun <K : Any, V> MapProperty<K, V>.invoke(vararg pairs: Pair<K, V>) = set(mapOf(*pairs))
internal inline operator fun <K : Any, V : Any> MapProperty<K, V>.set(key: K, value: V) = put(key, value)
internal inline operator fun <K : Any, V : Any> MapProperty<K, V>.set(key: K, value: Provider<V>) = put(key, value)

internal inline fun <reified T : Any> ObjectFactory.newInstance(vararg parameters: Any, build: T.() -> Unit)
    = newInstance<T>(*parameters).apply(build)

internal fun SourceSet.allSources() = sequence {
    yield(java)
    yield(resources)
    extensions.extensionsSchema.asSequence()
        .mapNotNull { extensions[it.name] }
        .filterIsInstance<SourceDirectorySet>()
        .let { yieldAll(it) }
}

internal fun Project.findProjectTree(): ProjectTree {
    val project = checkNotNull(parent) { "Stonecutter plugin was incorrectly applied. Refer to the documentation for more info."}
        .let { it.parent ?: it }
    val container = gradle.getContainer<ProjectTreeContainer>()
    return checkNotNull(container[project]) { "Tree for '${project.hierarchy}' not found in ${container.projects.keysToString()}" }
}

internal fun ProjectTree.getControllerImpl(): StonecutterControllerImpl =
    project.extensions.getByType<StonecutterControllerExtension>() as StonecutterControllerImpl

internal fun InputChanges.clearIfNotIncremental(vararg files: File) {
    if (isIncremental) return
    for (it in files) if (it.exists()) {
        it.deleteRecursively()
        it.mkdirs()
    }
}

internal val isIdeaSync: Boolean get() = System.getProperty("idea.sync.active", "false").toBoolean()
internal fun Gradle.requestTasks(tasks: Iterable<String>, path: String? = null, dir: File? = null) = startParameter.run {
    setTaskRequests(taskRequests + DefaultTaskExecutionRequest(tasks, path, dir))
}

internal val Project.projectDirectory get() = layout.projectDirectory.asFile
internal val Project.buildDirectory get() = layout.buildDirectory.asFile()