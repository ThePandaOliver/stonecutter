package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.tree.TreeBuilder
import org.gradle.kotlin.dsl.create
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.getByType

internal inline fun <reified T : ProjectContainerExtension<out Any>> Gradle.createContainer(): T =
    extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" })
internal inline fun <reified T : ProjectContainerExtension<out Any>> Gradle.getContainer(): T =
    extensions.getByType<T>()

internal abstract class ProjectContainerExtension<T> {
    val projects: MutableMap<ProjectHierarchy, T> = mutableMapOf()
    operator fun get(path: ProjectHierarchy): T? = projects[path]
    operator fun get(project: Project): T? = projects[project.hierarchy]
    fun register(path: ProjectHierarchy, value: T): Boolean =
        projects.putIfAbsent(path, value) == null
}

internal open class TreeBuilderContainer : ProjectContainerExtension<TreeBuilder>()