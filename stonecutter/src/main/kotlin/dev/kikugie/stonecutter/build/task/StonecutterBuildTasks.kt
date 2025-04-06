package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider

public interface StonecutterBuildTasks {
    public val prepare: Collection<TaskProvider<FileProcessingTask>>
    public val generate: Collection<TaskProvider<FileGeneratingTask>>
    public val merge: Collection<TaskProvider<Copy>>

    public fun prepareTaskName(src: SourceSet, dir: SourceDirectorySet): String = "stonecutterPrepare${taskSuffix(src, dir)}"
    public fun generateTaskName(src: SourceSet, dir: SourceDirectorySet): String = "stonecutterGenerate${taskSuffix(src, dir)}"
    public fun mergeTaskName(src: SourceSet, dir: SourceDirectorySet): String = "stonecutterMerge${taskSuffix(src, dir)}"

    private fun taskSuffix(src: SourceSet, dir: SourceDirectorySet) =
        (if (SourceSet.isMain(src)) "" else src.name.replaceFirstChar(Char::uppercase)) + dir.name.replaceFirstChar(Char::uppercase)
}