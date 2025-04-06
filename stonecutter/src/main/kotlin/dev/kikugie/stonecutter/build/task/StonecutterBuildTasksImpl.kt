package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

internal class StonecutterBuildTasksImpl {
    val prepareTasks: MutableCollection<TaskProvider<FileProcessingTask>> = mutableListOf()
    val generateTasks: MutableCollection<TaskProvider<FileGeneratingTask>> = mutableListOf()
    val mergeTasks: MutableCollection<TaskProvider<Copy>> = mutableListOf()

    fun prepareTaskName(src: SourceSet, dir: SourceDirectorySet) = "stonecutterPrepare${taskSuffix(src, dir)}"
    fun generateTaskName(src: SourceSet, dir: SourceDirectorySet) = "stonecutterGenerate${taskSuffix(src, dir)}"
    fun mergeTaskName(src: SourceSet, dir: SourceDirectorySet) = "stonecutterMerge${taskSuffix(src, dir)}"

    inline fun registerPrepareTask(project: Project, src: SourceSet, dir: SourceDirectorySet, crossinline config: FileProcessingTask.() -> Unit): TaskProvider<FileProcessingTask> {
        val task = project.tasks.register<FileProcessingTask>(prepareTaskName(src, dir)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }
        prepareTasks += task
        return task
    }

    inline fun registerGenerateTask(project: Project, src: SourceSet, dir: SourceDirectorySet, crossinline config: FileGeneratingTask.() -> Unit): TaskProvider<FileGeneratingTask> {
        val task = project.tasks.register<FileGeneratingTask>(generateTaskName(src, dir)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }
        generateTasks += task
        return task
    }

    inline fun registerMergeTask(project: Project, src: SourceSet, dir: SourceDirectorySet, crossinline config: Copy.() -> Unit): TaskProvider<Copy> {
        val task = project.tasks.register<Copy>(mergeTaskName(src, dir)) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }
        mergeTasks += task
        return task
    }

    private fun taskSuffix(src: SourceSet, dir: SourceDirectorySet) =
        (if (SourceSet.isMain(src)) "" else src.name.replaceFirstChar(Char::uppercase)) + dir.name.replaceFirstChar(Char::uppercase)
}