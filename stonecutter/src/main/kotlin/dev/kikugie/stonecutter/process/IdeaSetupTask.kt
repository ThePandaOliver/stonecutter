package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.readResource
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.nio.file.StandardOpenOption
import kotlin.collections.iterator
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists
import kotlin.io.path.writeText

internal abstract class IdeaSetupTask : DefaultTask() {
    companion object {
        val TEMPLATE: Result<String> by lazy { readResource("idea_config.xml") }
    }

    @get:Input
    abstract val versions: MapProperty<ProjectHierarchy, Iterable<String>>

    private val folder = project.rootDir.absoluteFile.resolve(".idea/runConfigurations").toPath()

    init {
        versions.convention(mutableMapOf())
    }

    @TaskAction
    fun run() {
        if (TEMPLATE.isFailure) return logger.error("Failed to read template configuration file", TEMPLATE.exceptionOrNull())
        if (folder.parent.notExists()) return logger.debug("No run configurations folder found")
        runCatching { folder.createDirectories() }.onFailure { return logger.error("Failed to create run configurations folder", it) }

        val files = mutableSetOf<String>()
        for ((project, versions) in versions()) files.addAll(configureTree(project, versions))

        for (file in folder.listDirectoryEntries()) if (file.fileName.toString().let { it.startsWith("Stonecutter") && it !in files })
            runCatching { file.deleteExisting() }.onFailure { logger.error("Failed to delete configuration file $file", it) }
    }

    private fun configureTree(project: ProjectHierarchy, versions: Iterable<String>) = buildList {
        writeConfiguration(project, "Reset active", "\"Reset active project\"")
        writeConfiguration(project, "Refresh active", "\"Refresh active project\"")
        for (name in versions)
            writeConfiguration(project, "Switch to $name", "\"Set active project to $name\"")
    }

    private fun MutableList<String>.writeConfiguration(project: ProjectHierarchy, name: String, task: String = name) {
        val filename = "Stonecutter${project.toString().replace(':', '_')}_${name.replace(' ', '_').replace(':', '_')}.xml"
        val file = folder.resolve(filename)
        if (file.exists()) {
            logger.debug("Configuration file $filename already exists")
            add(filename)
            return
        }

        val xml = TEMPLATE.getOrThrow()
            .replaceChecked("%FOLDER_NAME%", "Stonecutter${project.orBlank()}")
            .replaceChecked("%ENTRY_NAME%", name)
            .replaceChecked("%TASK_NAME%", "${project.orBlank()}:$task")
        runCatching {
            file.writeText(xml, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }.onSuccess {
            add(filename)
        }.onFailure {
            logger.error("Failed to write configuration file $filename", it)
        }
    }

    private fun String.replaceChecked(from: String, to: String) =
        replace(from, to.replace("\"", "&quot;"))
}