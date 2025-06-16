package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

public sealed interface SCSwitchTask : Task

public abstract class SCExternalSwitchTask : DefaultTask(), SCSwitchTask {
    @get:InputFile
    public abstract val file: RegularFileProperty

    @get:Input
    public abstract val version: Property<String>

    @TaskAction
    public fun run() {
        file.asFile().toPath().writeText(version(), Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)
    }
}

@OptIn(StonecutterInternalAPI::class)
public abstract class SCScriptSwitchTask : DefaultTask(), SCSwitchTask {
    @get:InputFile
    public abstract val script: RegularFileProperty

    @get:Input
    public abstract val version: Property<String>

    @get:Input
    public abstract val manager: Property<Class<out StonecutterControllerManager>>

    @TaskAction
    public fun run() {
        val instance = when (val kcls = manager().kotlin) {
            StonecutterControllerManager.Kotlin::class -> StonecutterControllerManager.Kotlin
            StonecutterControllerManager.Groovy::class -> StonecutterControllerManager.Groovy
            else -> error("Invalid controller manager class ${kcls.qualifiedName}")
        }
        instance.update(script.asFile().toPath(), version())
    }
}