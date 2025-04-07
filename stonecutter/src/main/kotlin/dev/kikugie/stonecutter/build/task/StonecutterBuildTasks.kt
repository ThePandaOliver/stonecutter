package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import dev.kikugie.stonecutter.util.TaskProviderMap
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import java.io.File

public interface StonecutterBuildTasks {
    public val prepare: TaskProviderMap<FileProcessingTask>
    public val generate: TaskProviderMap<FileGeneratingTask>
    public val merge: TaskProviderMap<Copy>
    public val processedCacheDir: File
    public val generatedSourcesDir: File

    public fun prepareTaskName(src: SourceSet): String = "stonecutterPrepare${taskSuffix(src)}"
    public fun generateTaskName(src: SourceSet): String = "stonecutterGenerate${taskSuffix(src)}"
    public fun mergeTaskName(src: SourceSet): String = "stonecutterMerge${taskSuffix(src)}"
    public fun taskSuffix(src: SourceSet): String =
        if (SourceSet.isMain(src)) "" else src.name.replaceFirstChar(Char::uppercase)

    public fun configureSource(src: SourceSet)
}