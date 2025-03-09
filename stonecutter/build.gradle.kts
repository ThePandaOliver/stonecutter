@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalPathApi::class)

import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
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
    alias(libs.plugins.shadow)
    alias(libs.plugins.gradle.publishing)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.serialization)
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
    api(project(":stitcher"))
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kaml)

    testImplementation(libs.bundles.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<AbstractDokkaLeafTask>().configureEach {
    moduleName.set("Stonecutter Gradle")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

tasks.compileKotlin {
    explicitApiMode = ExplicitApiMode.Strict
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_0
        apiVersion = KotlinVersion.KOTLIN_2_0
        jvmTarget.set(JvmTarget.JVM_16)
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.shadowJar {
    archiveBaseName.set("shadow")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaJavadoc"))
}

tasks.all {
    if (this is Jar || this is DokkaTask || this is KotlinCompile)
        dependsOn(rootProject.tasks.findByName("updateVersion"))
}

tasks.withType<AbstractDokkaLeafTask> {
    moduleName = "Stonecutter Gradle"
    dokkaSourceSets.configureEach {
        samples.from("src/samples/kotlin")
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
        register("mavenJava", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "stonecutter"
            version = project.version.toString()
            from(components["java"])
        }
    }
}

gradlePlugin {
    website = "https://stonecutter.kikugie.dev/"
    vcsUrl = "https://github.com/stonecutter-versioning/stonecutter"

    plugins {
        create("stonecutter") {
            id = "dev.kikugie.stonecutter"
            implementationClass = "dev.kikugie.stonecutter.StonecutterPlugin"
            displayName = "Stonecutter"
            description = "Modern Gradle plugin for multi-version management"
            tags = setOf("minecraft", "mods")
        }
    }
}