package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.StonecutterPlugin
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

@StonecutterInternalAPI
public abstract class SCIdeaConfigTask : DefaultTask() {
    private companion object {
        val PATTERN = Regex("[ :]")
        val TEMPLATE: Result<String> by lazy { readResource("idea_config.xml") }
        fun readResource(path: String): Result<String> = runCatching {
            StonecutterPlugin::class.java.classLoader.getResourceAsStream(path)?.use { it.reader().readText() }
                ?: error("Resource $path not found")
        }
    }

    @get:Input
    public abstract val versions: MapProperty<String, List<String>>

    @get:OutputFiles
    public abstract val configurations: ConfigurableFileTree

    private val folder = project.rootDir.absoluteFile.resolve(".idea/runConfigurations").toPath()

    init {
        versions.convention(mutableMapOf())
        configurations.from(folder)
        configurations.include { it.file.name.startsWith("Stonecutter") }
        onlyIf { TEMPLATE.isSuccess }
    }
    @TaskAction
    public fun run() {
        val files = buildSet {
            for ((project, versions) in versions())
                for (version in versions)
                    writeConfiguration(ProjectHierarchy(project), version).let(::add)
        }

        for (file in folder.listDirectoryEntries()) {
            if (file.fileName.name.let { it.startsWith("Stonecutter") && it !in files })
                file.runCatching {  deleteIfExists() }
        }
    }

    private fun writeConfiguration(project: ProjectHierarchy, version: String): String {
        val name = configuration(project, version)
        val file = folder.resolve(name)
        if (file.exists()) return name

        val xml = TEMPLATE.getOrThrow()
            .replaceChecked("%FOLDER_NAME%", "Stonecutter${project.orBlank()}")
            .replaceChecked("%ENTRY_NAME%", "Switch to $version")
            .replaceChecked("%TASK_NAME%", "${project.orBlank()}:stonecutterSwitchTo$version")
        file.writeText(xml, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        return name
    }

    private fun configuration(project: ProjectHierarchy, version: String) =
        "Stonecutter${project.orBlank().formatPath()}_switchTo${version.formatPath()}.xml"
    private fun String.formatPath(): String = replace(PATTERN, "_")
    private fun String.replaceChecked(from: String, to: String) =
        replace(from, to.replace("\"", "&quot;"))
}