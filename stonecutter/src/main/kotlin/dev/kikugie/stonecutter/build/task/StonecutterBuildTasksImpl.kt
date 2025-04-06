package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

internal class StonecutterBuildTasksImpl : StonecutterBuildTasks {
    override val prepare: MutableCollection<TaskProvider<FileProcessingTask>> = mutableListOf()
    override val generate: MutableCollection<TaskProvider<FileGeneratingTask>> = mutableListOf()
    override val merge: MutableCollection<TaskProvider<Copy>> = mutableListOf()

    inline fun registerPrepareTask(project: Project, src: SourceSet, dir: SourceDirectorySet, crossinline config: FileProcessingTask.() -> Unit): TaskProvider<FileProcessingTask> {
        val task = project.tasks.register<FileProcessingTask>(prepareTaskName(src, dir)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }
        prepare += task
        return task
    }

    inline fun registerGenerateTask(project: Project, src: SourceSet, dir: SourceDirectorySet, crossinline config: FileGeneratingTask.() -> Unit): TaskProvider<FileGeneratingTask> {
        val task = project.tasks.register<FileGeneratingTask>(generateTaskName(src, dir)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }
        generate += task
        return task
    }

    inline fun registerMergeTask(project: Project, src: SourceSet, dir: SourceDirectorySet, crossinline config: Copy.() -> Unit): TaskProvider<Copy> {
        val task = project.tasks.register<Copy>(mergeTaskName(src, dir)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }
        merge += task
        return task
    }
}