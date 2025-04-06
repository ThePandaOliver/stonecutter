package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

internal abstract class ControllerExternalUpdateTask : DefaultTask() {
    @get:InputFile
    abstract val file: RegularFileProperty

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun run() {
        file.asFile().toPath().writeText(version(), Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)
    }
}