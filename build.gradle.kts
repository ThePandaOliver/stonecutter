import tasks.HallOfFameTask
import tasks.UpdateVersionTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.node)
}

group = property("group").toString()
version = property("version").toString()

repositories {
    mavenCentral()
}

dependencies {
    dokka(project(":stonecutter"))
    dokka(project(":stitcher"))
}

dokka {
    moduleName = "Stonecutter KDoc"

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }
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

    configFile = file("docs/hof/config.yml")
    cacheFile = file("docs/hof/search.cache.yml")
    templateFile = file("docs/hof/template.md")
    outputFiles = files("docs/index.md")
}

//tasks.register<Sync>("syncDokkaPages") {
//    from(fileTree("build/dokka/htmlMultiModule"))
//    into(file("docs/public/dokka"))
//
//    dependsOn("dokkaHtmlMultiModule")
//}
//
//tasks.register<NpmTask>("buildDocPages") {
//    args = listOf("run", "docs:build")
//    mustRunAfter("updateVersion", "updateHallOfFame", "syncDokkaPages")
//}
//
//tasks.register("composeDocPages") {
//    dependsOn("updateVersion", "updateHallOfFame", "syncDokkaPages", "buildDocPages")
//}

node {
    download = true
    version = "23.11.0"
}