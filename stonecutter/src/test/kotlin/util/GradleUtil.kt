package util

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

internal const val SETTINGS = "settings.gradle.kts"
internal const val CONTROLLER = "stonecutter.gradle.kts"
internal const val BUILD = "build.gradle.kts"

internal const val SETTINGS_TEMPLATE = "plugins { id(\"dev.kikugie.stonecutter\") }"

internal infix fun File.with(@Language("kotlin") content: String): Unit =
    toPath().writeText(content, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)

internal fun resource(path: String): InputStream = TreeBuildingTest::class.java.classLoader.getResourceAsStream(path)
    ?: throw FileNotFoundException(path)

internal fun build(dir: File, vararg args: String): BuildResult = GradleRunner.create()
    .withProjectDir(dir)
    .withArguments(*args)
    .withPluginClasspath()
    .build()

internal inline fun build(dir: File, vararg args: String, action: BuildResult.() -> Unit) = with(build(dir, *args), action)