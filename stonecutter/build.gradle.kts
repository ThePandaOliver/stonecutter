@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalPathApi::class)

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.io.path.ExperimentalPathApi

plugins {
    idea
    java
    `kotlin-dsl`
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.validator)
//    alias(libs.plugins.kotlin.ksp)
//    alias(libs.plugins.extra.kdoclink)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(path = ":semver"))
    api(project(path = ":stitcher"))
    implementation(libs.bundles.stonecutter)
}

//kdoclink {
//    fun wiki(page: String) = "https://stonecutter.kikugie.dev/wiki/$page"
//
//    annotation = "dev.kikugie.stonecutter.SCDocumentation"
//    this["settings"] = wiki("start/settings")
//    this["settings.vcs"] = wiki("start/settings#version-reset-point")
//    this["settings.create"] = wiki("start/settings#specifying-versions")
//    this["settings.json"] = wiki("config/params")
//
//    this["swaps"] = wiki("config/params#string-swaps")
//    this["swaps.spec"] = wiki("config/params#swap-specification")
//
//    this["consts"] = wiki("config/params#condition-constants")
//    this["consts.spec"] = wiki("config/params#constant-specification")
//    this["consts.choice"] = wiki("config/params#choice-selector")
//
//    this["deps"] = wiki("config/params#condition-dependencies")
//    this["deps.spec"] = wiki("config/params#dependency-specification")
//
//    this["utility"] = wiki("guide/setup#checking-versions")
//}

apiValidation {
    ignoredPackages += "stonecutter_samples"
    nonPublicMarkers += "dev.kikugie.stonecutter.StonecutterInternalAPI"
}

dokka {
    moduleName = "Stonecutter Gradle"

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }

    dokkaPublications.html {
        suppressInheritedMembers = true
        suppressObviousFunctions = true
    }

    dokkaSourceSets.named("main") {
        reportUndocumented = true
        skipEmptyPackages = true

        sourceLink {
            localDirectory = file("src/main/kotlin")
            remoteLineSuffix = "#L"
            remoteUrl("https://codeberg.org/stonecutter/stonecutter/src/branch/0.7/stonecutter/")
        }

        externalDocumentationLinks.register("gradle-kotlin-dsl") {
            url("https://docs.gradle.org/current/kotlin-dsl/")
            packageListUrl("https://docs.gradle.org/current/kotlin-dsl/gradle/package-list")
        }

        externalDocumentationLinks.register("kotlin-stdlib") {
            url("https://kotlinlang.org/api/core/")
        }

        externalDocumentationLinks.register("kotlinx-serialization") {
            url("https://kotlinlang.org/api/kotlinx.serialization/")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

tasks {
    register<ShadowJar>("slimJar") {
        group = "build"
        archiveClassifier = "slim"
        configurations = project.configurations.runtimeClasspath.map(::listOf)

        from(sourceSets.main.map(SourceSet::getOutput))
        dependencies {
            include(project(":semver"))
            include(project(":stitcher"))
        }
    }

    shadowJar {
        archiveClassifier = ""
    }

    compileKotlin {
        explicitApiMode = ExplicitApiMode.Strict
        compilerOptions {
            languageVersion = KotlinVersion.KOTLIN_2_1
            apiVersion = KotlinVersion.KOTLIN_2_1
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Jar> {
        dependsOn(":updateVersion")
    }

    withType<DokkaTask> {
        dependsOn(":updateVersion")
    }

    withType<KotlinCompile> {
        dependsOn(":updateVersion")
    }
}

publishing {
    repositories {
        maven {
            name = "kikugieMaven"
            url = uri("https://maven.kikugie.dev/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create("basic", BasicAuthentication::class)
            }
        }
    }

    publications {
        register<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = "stonecutter"
            version = project.version.toString()
            from(components["java"])
            artifact(tasks.named("slimJar"))
        }
    }
}

gradlePlugin {
    website = "https://stonecutter.codeberg.page/"
    vcsUrl = "https://codeberg.org/stonecutter/stonecutter"

    plugins {
        create("stonecutter") {
            id = "dev.kikugie.stonecutter"
            implementationClass = "dev.kikugie.stonecutter.StonecutterPlugin"
            displayName = "Stonecutter"
            description = "Modern Gradle plugin for multi-version management"
        }
    }
}