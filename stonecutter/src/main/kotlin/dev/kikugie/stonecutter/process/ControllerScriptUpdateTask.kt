package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

internal abstract class ControllerScriptUpdateTask : DefaultTask() {
    @get:InputFile
    abstract val script: RegularFileProperty

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val manager: Property<Class<out StonecutterControllerManager>>

    @TaskAction
    fun run() {
        val instance = when (val kcls = manager().kotlin) {
            StonecutterControllerManager.Kotlin::class -> StonecutterControllerManager.Kotlin
            StonecutterControllerManager.Groovy::class -> StonecutterControllerManager.Groovy
            else -> error("Invalid controller manager class ${kcls.qualifiedName}")
        }
        instance.update(script.asFile().toPath(), version())
    }
}