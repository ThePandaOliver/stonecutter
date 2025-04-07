import com.github.gradle.node.npm.task.NpmTask
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.AbstractDokkaParentTask
import tasks.HallOfFameTask
import tasks.UpdateVersionTask
import java.net.URI

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.node)
}

group = property("group").toString()
version = property("version").toString()

val String.URL get() = URI.create(this).toURL()

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(libs.dokka.base)
    }
}

repositories {
    mavenCentral()
}

tasks.register<UpdateVersionTask>("updateVersion") {
    group = "documentation"

    val ver = project.version.toString()
    version = ver
    replacements {
        file("stonecutter/src/main/kotlin/dev/kikugie/stonecutter/Utilities.kt") replace "val STONECUTTER: String = .+\"" with "val STONECUTTER: String = \"$ver\""
        file("stonecutter/src/main/kotlin/dev/kikugie/stonecutter/StonecutterPlugin.kt") replace "VERSION: String = \".+\"" with "VERSION: String = \"$ver\""
        file("docs/.vitepress/config.mts") replace "latestVersion: \".+\"" with "latestVersion: \"$ver\""
        file("docs/wiki/start/settings.md") replace listOf(
            "stonecutter\"\\ version \".+\"" to "stonecutter\" version \"$ver\"",
            "stonecutter\"\\) version \".+\"" to "stonecutter\") version \"$ver\""
        )
    }
}

tasks.register<HallOfFameTask>("updateHallOfFame") {
    group = "documentation"
    description = "Updates the Hall of Fame"

    file(".env").takeIf { it.exists() }
        ?.useLines { it.find { it.startsWith("GITHUB_TOKEN=") }?.substringAfter("=") }
        ?.let { githubToken.set(it) }

    configFile.set(file("docs/hof/config.yml"))
    cacheFile.set(file("docs/hof/search.cache.yml"))
    templateFile.set(file("docs/hof/template.md"))
    outputFiles.set(files("docs/index.md"))
}

tasks.register<Sync>("syncDokkaPages") {
    from(fileTree("build/dokka/htmlMultiModule"))
    into(file("docs/public/dokka"))

    dependsOn("dokkaHtmlMultiModule")
}

tasks.register<NpmTask>("buildDocPages") {
    args = listOf("run", "docs:build")
    mustRunAfter("updateVersion", "updateHallOfFame", "syncDokkaPages")
}

tasks.register("composeDocPages") {
    dependsOn("updateVersion", "updateHallOfFame", "syncDokkaPages", "buildDocPages")
}

tasks.withType<AbstractDokkaParentTask> {
    moduleName = "Stonecutter KDoc"

    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }
}

node {
    download = true
    version = "23.11.0"
}

subprojects {
    tasks.withType<AbstractDokkaLeafTask> {
        dokkaSourceSets.configureEach {
            reportUndocumented = true
            skipEmptyPackages = true
            suppressObviousFunctions = true
            suppressInheritedMembers = true

            sourceLink {
                localDirectory.set(projectDir)
                remoteUrl.set("https://codeberg.org/stonecutter/stonecutter/src/branch/0.6/${project.name}/".URL)
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLink {
                url = "https://docs.gradle.org/current/kotlin-dsl/".URL
                packageListUrl = "https://docs.gradle.org/current/kotlin-dsl/gradle/package-list".URL
            }

            externalDocumentationLink {
                url = "https://kotlinlang.org/api/core/".URL
            }

            externalDocumentationLink {
                url = "https://kotlinlang.org/api/kotlinx.serialization/".URL
            }
        }
    }
}