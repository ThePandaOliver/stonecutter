package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.controller.flag.IMPLICIT_RECEIVER
import dev.kikugie.stonecutter.process.FileGeneratingTask
import dev.kikugie.stonecutter.process.FileProcessingData
import dev.kikugie.stonecutter.process.FileProcessingTask
import dev.kikugie.stonecutter.util.MutableTaskProviderMap
import dev.kikugie.stonecutter.util.allSources
import dev.kikugie.stonecutter.util.buildDirectory
import dev.kikugie.stonecutter.util.debug
import dev.kikugie.stonecutter.util.logger
import dev.kikugie.stonecutter.util.projectDirectory
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File
import kotlin.reflect.KClass

internal class StonecutterBuildTasksImpl(val ext: StonecutterBuildImpl) : StonecutterBuildTasks {
    override val prepare: MutableTaskProviderMap<FileProcessingTask> = mutableMapOf()
    override val generate: MutableTaskProviderMap<FileGeneratingTask> = mutableMapOf()
    override val merge: MutableTaskProviderMap<Copy> = mutableMapOf()
    override val processedCacheDir: File get() = ext.project.buildDirectory.resolve("stonecutter-cache/sources")
    override val generatedSourcesDir: File get() = ext.project.buildDirectory.resolve("generated/stonecutter")
    private val logger by logger("StonecutterBuildTasks")

    inline fun configurePrepareTask(src: SourceSet, crossinline config: FileProcessingTask.(Boolean) -> Unit) : TaskProvider<FileProcessingTask> {
        val name = prepareTaskName(src)
        val init = name !in prepare
        return prepare.getOrPut(name) { registerDefaultTask(name, FileProcessingTask::class) }.apply { configure { config(init) } }
    }

    inline fun configureGenerateTask(src: SourceSet, crossinline config: FileGeneratingTask.(Boolean) -> Unit) : TaskProvider<FileGeneratingTask> {
        val name = generateTaskName(src)
        val init = name !in generate
        return generate.getOrPut(name) { registerDefaultTask(name, FileGeneratingTask::class) }.apply { configure { config(init) } }
    }

    inline fun configureMergeTask(src: SourceSet, crossinline config: Copy.(Boolean) -> Unit) : TaskProvider<Copy> {
        val name = mergeTaskName(src)
        val init = name !in merge
        return merge.getOrPut(name) { registerDefaultTask(name, Copy::class) }.apply { configure { config(init) } }
    }

    private fun <T : Task> registerDefaultTask(name: String, cls: KClass<T>): TaskProvider<T> = ext.project.tasks.register(name, cls) {
        group = "stonecutter-impl"
        description = "Internal Stonecutter task. Do not call manually."
    }

    override fun configureSource(src: SourceSet) {
        val sourceDirectories = attachVersionedSources(src)
        if (sourceDirectories.isEmpty()) return logger.debug { "No more sources found for ${sourceID(src)}" }
        else logger.debug { "Found ${sourceDirectories.size} sources for ${sourceID(src)}" }

        val prepareTask = configurePrepareTask(src) {
            sources.from(sourceDirectories)
            if (it) {
                root.set(ext.parent.projectDirectory.resolve("src"))
                caches.set(processedCacheDir.resolve(src.name))
                project.provider { ext.data.asProcessingData(ext.flags[IMPLICIT_RECEIVER], ext.current.version) }
                    .let<Provider<FileProcessingData>, Unit>(parameters::set)
            }
        }

        configureGenerateTask(src) {
            sources.from(sourceDirectories)
            if (it) {
                root.set(ext.parent.projectDirectory.resolve("src/${src.name}"))
                source.set(project.projectDirectory.resolve("src/${src.name}"))
                cache.set(generatedSourcesDir.resolve(src.name))

                excludes.from(project.layout.projectDirectory.dir("src/${src.name}").asFileTree)
                processed.set(processedCacheDir.resolve(src.name))
                generated.set(generatedSourcesDir.resolve(src.name))
                dependsOn(prepareTask)
            }
        }

        if (mergeTaskName(src) !in merge) configureMergeTask(src) {
            from(processedCacheDir.resolve(src.name))
            into(ext.parent.projectDirectory.resolve("src/${src.name}"))
            dependsOn(prepareTask)
        }
    }

    private fun attachVersionedSources(src: SourceSet): Set<File> {
        val branchSrc: File = ext.parent.projectDirectory.resolve("src")
        val versionSrc: File = ext.project.projectDirectory.resolve("src")
        val prepareTask = "${ext.project.path}:${prepareTaskName(src)}"
        return src.allSources().flatMap { set ->
            val matchingDirs = set.sourceDirectories
                .map { it.relativeTo(versionSrc) }
                .filterNot { it.startsWith("..") }

            val sourceDirs = matchingDirs.mapNotNull { branchSrc.resolve(it).takeUnless(src.allSource::contains) }
            if (ext.current.isActive) {
                set.srcDirs(ext.project.files(sourceDirs))
                logger.debug { "Adding active sources to ${sourceID(src)}:\n${sourceDirs.joinToString("\n") { "\t- $it" }}" }
            }
            else matchingDirs.mapNotNull { generatedSourcesDir.resolve(it).takeUnless(src.allSource::contains) }.let {
                ext.project.files(it).builtBy(prepareTask).let(set::srcDir)
                logger.debug { "Adding generated sources to ${sourceID(src)}:\n${it.joinToString("\n") { "\t- $it" }}" }
            }
            sourceDirs
        }.toSet()
    }

    private fun sourceID(src: SourceSet): String = "${ext.project.path} ${src.name}"
}