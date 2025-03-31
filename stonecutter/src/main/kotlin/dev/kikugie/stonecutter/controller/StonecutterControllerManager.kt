package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.Identifier
import org.gradle.api.Project
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal interface StonecutterControllerManager {
    val filename: String
    fun create(file: Path, version: Identifier)
    fun update(file: Path, version: Identifier) {
        val original = file.readText(Charsets.UTF_8)
        val updated = original.replaceVersion(version)
        if (original != updated) file overwrite updated
    }

    companion object {
        private val PATTERN = Regex("stonecutter[\\s.]active\\s*\\(?[\"'](\\S+)[\"']\\)?")
        private infix fun Path.overwrite(text: String) =
            writeText(text, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        private fun String.replaceVersion(version: Identifier) = replace(PATTERN) { it.value.replace(it.groupValues[1], version) }

        fun Project.getController() = when (buildFile.name) {
            Groovy.filename -> Groovy
            Kotlin.filename -> Kotlin
            else -> null
        }
    }

    object Groovy : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle"

        override fun create(file: Path, version: Identifier) = file overwrite """
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

    object Kotlin : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle.kts"
        override fun create(file: Path, version: Identifier) = file overwrite """
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