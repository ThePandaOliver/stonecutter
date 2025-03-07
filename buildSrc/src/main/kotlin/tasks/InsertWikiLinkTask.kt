package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

abstract class InsertWikiLinkTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    abstract val links: MapProperty<String, Pair<String, String>>

    @get:Input
    abstract val domain: Property<String>

    @Suppress("unused")
    val output: ConfigurableFileCollection
        @OutputFiles get() = files

    @TaskAction
    fun run() = files.forEach(::process)

    fun link(key: String, url: String, text: String = "Wiki page") = links.put(key, text to url)

    private fun process(file: File) = file.remapLines { lines ->
        val links = mutableListOf<String>()
        for (line in lines) when {
            line.trimStart().startsWith("//") && "link:" in line -> links += line.substringAfter("link:").trim()
                .also { check(it.isNotBlank()) { "Invalid link: $line" }; appendLine(line) }
            line.trimStart().startsWith("/**") && links.isNotEmpty() -> appendLine(insertLinks(line, lines, links))
            else -> { appendLine(line); links.clear() }
        }
    }

    private fun insertLinks(current: String, lines: Iterator<String>, keys: List<String>) = buildList {
        add(current)
        for (line in lines) when {
            "@see <a href=" in line && domain.get() in line -> continue
            "*/" in line -> { add(line); break }
            else -> add(line)
        }
        if (keys.isEmpty()) return@buildList
        val spaces = last<String>().takeWhile { it.isWhitespace() }
        for (key in keys) {
            val (text, url) = links.get()[key] ?: continue
            val link = "<a href=\"${URI(domain.get()).resolve(url)}\">$text</a>"
            add(size - 1, "$spaces* @see $link")
        }
    }.joinToString("\n")

    private inline fun File.remapLines(block: StringBuilder.(Iterator<String>) -> Unit) = useLines { lines ->
        buildString { block(lines.iterator()) }
    }.let { writeText(it) }
}