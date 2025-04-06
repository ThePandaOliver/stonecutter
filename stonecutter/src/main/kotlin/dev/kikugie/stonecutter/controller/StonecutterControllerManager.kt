package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.Identifier
import org.gradle.api.Project
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.readText
import kotlin.io.path.writeText

public interface StonecutterControllerManager {
    public val filename: String
    public fun create(file: Path, version: Identifier)
    public fun update(file: Path, version: Identifier) {
        val original = file.readText(Charsets.UTF_8)
        val updated = original.replaceVersion(version)
        if (original != updated) file overwrite updated
    }

    public companion object {
        private val PATTERN = Regex("stonecutter[\\s.]active\\s*\\(?[\"'](\\S+)[\"']\\)?")
        private infix fun Path.overwrite(text: String) =
            writeText(text, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        private fun String.replaceVersion(version: Identifier) = replace(PATTERN) { it.value.replace(it.groupValues[1], version) }

        public fun Project.getController(): StonecutterControllerManager? = when (buildFile.name) {
            Groovy.filename -> Groovy
            Kotlin.filename -> Kotlin
            else -> null
        }
    }

    public object Groovy : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle"

        override fun create(file: Path, version: Identifier): Unit = file overwrite """
            plugins {
                id "dev.kikugie.stonecutter"
            }
            stonecutter.active "$version"
            
            stonecutter.registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { 
                setGroup "project"
                ofTask "build"
            }
        """.trimIndent()
    }

    public object Kotlin : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle.kts"
        override fun create(file: Path, version: Identifier): Unit = file overwrite """
            plugins {
                id("dev.kikugie.stonecutter")
            }
            stonecutter active "$version"
            
            stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { 
                group = "project"
                ofTask("build")
            }
        """.trimIndent()
    }
}