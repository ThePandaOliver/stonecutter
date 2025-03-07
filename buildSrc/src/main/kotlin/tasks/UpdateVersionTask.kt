package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.intellij.lang.annotations.Language
import java.io.File

private typealias Replacements = MutableMap<File, MutableList<Pair<String, String>>>

abstract class UpdateVersionTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    abstract val replacements: MapProperty<File, MutableList<Pair<String, String>>>

    @Suppress("unused")
    val output: ConfigurableFileCollection
        @OutputFiles get() = files

    fun replacements(configuration: Builder.() -> Unit) {
        val map: Replacements = mutableMapOf()
        Builder(map).configuration()
        replacements.set(map)
        replacements.disallowChanges()
        files.setFrom(map.keys)
        files.disallowChanges()
    }

    @TaskAction
    fun run() = files.forEach {
        val entries = replacements.get()[it]
            ?: return@forEach logger.warn("No replacement found for $it")

        val text = it.readText()
        var modified = text
        for ((pattern, replacement) in entries)
            modified = modified.replace(pattern.toRegex(), replacement)
        if (modified == text) logger.info("No changes found in $it")
        else it.writeText(modified).also { logger.info("Updated $it") }
    }

    class Builder(internal val map: Replacements) {
        infix fun File.replace(@Language("RegExp") pattern: String) =
            Incomplete(this, pattern)

        infix fun File.replace(items: Iterable<Pair<String, String>>) = items.forEach { (pattern, replacement) ->
            map.getOrPut(this) { mutableListOf() } += pattern to replacement
        }

        inner class Incomplete(private val file: File, private val pattern: String) {
            infix fun with(replacement: String) {
                map.getOrPut(file) { mutableListOf() } += pattern to replacement
            }
        }
    }
}