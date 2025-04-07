package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.StonecutterDevAPI
import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import dev.kikugie.stonecutter.util.TaskProviderMap
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import java.io.File

/**
 * Provides structured access to tasks created by [StonecutterBuild][dev.kikugie.stonecutter.build.StonecutterBuildExtension].
 */
@StonecutterDevAPI
public interface StonecutterBuildTasks {
    /**
     * Comment processing tasks for each source set in the project.
     * @see FileProcessingTask
     */
    @StonecutterDevAPI public val prepare: TaskProviderMap<FileProcessingTask>

    /**
     * Versioned source generating tasks for each source set in the project.
     * @see FileGeneratingTask
     */
    @StonecutterDevAPI public val generate: TaskProviderMap<FileGeneratingTask>

    /**Version switch merging tasks for each source set in the project.*/
    @StonecutterDevAPI public val merge: TaskProviderMap<Copy>

    /**`versions/**/build/stonecutter-cache/sources/`*/
    @StonecutterDevAPI public val processedCacheDir: File

    /**`versions/**/build/generated/stonecutter/`*/
    @StonecutterDevAPI public val generatedSourcesDir: File

    @StonecutterDevAPI
    public fun prepareTaskName(src: SourceSet): String = "stonecutterPrepare${taskSuffix(src)}"
    @StonecutterDevAPI
    public fun generateTaskName(src: SourceSet): String = "stonecutterGenerate${taskSuffix(src)}"
    @StonecutterDevAPI
    public fun mergeTaskName(src: SourceSet): String = "stonecutterMerge${taskSuffix(src)}"

    /**
     * @return Empty string if [src] is `main`, otherwise its capitalised name
     */
    @StonecutterDevAPI
    public fun taskSuffix(src: SourceSet): String =
        if (SourceSet.isMain(src)) "" else src.name.replaceFirstChar(Char::uppercase)

    @StonecutterDevAPI
    public fun configureSource(src: SourceSet)
}