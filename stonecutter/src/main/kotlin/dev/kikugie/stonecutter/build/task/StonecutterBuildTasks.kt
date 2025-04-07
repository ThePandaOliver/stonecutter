package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import dev.kikugie.stonecutter.util.TaskProviderMap
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import java.io.File

/**
 * Provides structured access to tasks created by [dev.kikugie.stonecutter.build.StonecutterBuildExtension].
 */
public interface StonecutterBuildTasks {
    /**
     * Comment processing tasks for each source set in the project.
     * @see FileProcessingTask
     */
    public val prepare: TaskProviderMap<FileProcessingTask>

    /**
     * Versioned source generating tasks for each source set in the project.
     * @see FileGeneratingTask
     */
    public val generate: TaskProviderMap<FileGeneratingTask>

    /**
     * Version switch merging tasks for each source set in the project.
     */
    public val merge: TaskProviderMap<Copy>
    /**`versions/**/build/stonecutter-cache/sources/`*/
    public val processedCacheDir: File
    /**`versions/**/build/generated/stonecutter/`*/
    public val generatedSourcesDir: File

    /**
     * @return `stonecutterPrepare[[taskSuffix]]
     */
    public fun prepareTaskName(src: SourceSet): String = "stonecutterPrepare${taskSuffix(src)}"
    /**
     * @return `stonecutterGenerate[[taskSuffix]]
     */
    public fun generateTaskName(src: SourceSet): String = "stonecutterGenerate${taskSuffix(src)}"
    /**
     * @return `stonecutterMerge[[taskSuffix]]
     */
    public fun mergeTaskName(src: SourceSet): String = "stonecutterMerge${taskSuffix(src)}"

    /**
     * @return Empty string if [src] is `main`, otherwise its capitalised name
     */
    public fun taskSuffix(src: SourceSet): String =
        if (SourceSet.isMain(src)) "" else src.name.replaceFirstChar(Char::uppercase)

    public fun configureSource(src: SourceSet)
}