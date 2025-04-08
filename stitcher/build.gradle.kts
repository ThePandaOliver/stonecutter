import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.kotlin.dokka)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.kotlin.serialization)

    testImplementation(libs.kaml)
    testImplementation(libs.bundles.test)
}

dokka {
    moduleName = "Stitcher Processor"
    dokkaPublications.html {
        suppressInheritedMembers = true
        suppressObviousFunctions = true
    }

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }

    dokkaSourceSets.main {
        reportUndocumented = false
        skipEmptyPackages = true

        sourceLink {
            localDirectory = file("src/main/kotlin")
            remoteLineSuffix = "#L"
            remoteUrl("https://codeberg.org/stonecutter/stonecutter/src/branch/0.7/stitcher/")
        }

        externalDocumentationLinks.register("kotlin-stdlib") {
            url("https://kotlinlang.org/api/core/")
        }

        externalDocumentationLinks.register("kotlinx-serialization") {
            url("https://kotlinlang.org/api/kotlinx.serialization/")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_16)
    }
}
//
//publishing {
//    repositories {
//        maven {
//            name = "kikugieMaven"
//            url = uri("https://maven.kikugie.dev/snapshots")
//            credentials(PasswordCredentials::class)
//            authentication {
//                create("basic", BasicAuthentication::class)
//            }
//        }
//    }
//
//    publications {
//        register("mavenJava", MavenPublication::class) {
//            groupId = project.group.toString()
//            artifactId = "stitcher"
//            version = project.version.toString()
//            artifact(tasks.getByName("jar"))
//        }
//    }
//}