package dev.kikugie.stonecutter.process.v2

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

internal abstract class FileProcessingTask : DefaultTask() {
    @get:InputDirectory @get:Incremental
    abstract val sources: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun run(inputs: InputChanges) {
        val changes = inputs.getFileChanges(sources).toList()
        for (c in changes) println("File ${c.file} was ${c.changeType}")
    }
}