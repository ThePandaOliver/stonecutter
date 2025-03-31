package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.tree.ProjectTree
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
    operator fun get(project: Project): T? = projects[project.hierarchy]
    operator fun get(path: ProjectHierarchy): T? = projects[path]
    fun register(path: ProjectHierarchy, value: T): Boolean =
        projects.putIfAbsent(path, value) == null

    fun getNearest(project: Project): T? = getNearest(project.hierarchy)
    fun getNearest(path: ProjectHierarchy): T? {
        var hierarchy = path
        while (true) {
            val content = projects[hierarchy]
            if (content != null) return content
            else if (hierarchy.isEmpty()) return null
            else hierarchy -= hierarchy.last()
        }
    }
}

internal open class TreeBuilderContainer : ProjectContainerExtension<TreeBuilder>()
internal open class ProjectTreeContainer : ProjectContainerExtension<ProjectTree>()