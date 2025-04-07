package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.data.tree.TreeBuilder
import org.gradle.kotlin.dsl.create
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.getByType

internal inline fun <reified T : GradleContainerExtension<out Any>> Gradle.createContainer(): T =
    extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" })
internal inline fun <reified T : GradleContainerExtension<out Any>> Gradle.getContainer(): T =
    extensions.getByType<T>()

internal abstract class GradleContainerExtension<T> {
    val projects: MutableMap<ProjectHierarchy, T> = mutableMapOf()
    operator fun get(project: Project): T? = projects[project.hierarchy]
    operator fun get(path: ProjectHierarchy): T? = projects[path]
    fun register(path: ProjectHierarchy, value: T): Boolean =
        projects.putIfAbsent(path, value) == null
}

internal open class TreeBuilderContainer : GradleContainerExtension<TreeBuilder>()
internal open class ProjectTreeContainer : GradleContainerExtension<ProjectTree>()