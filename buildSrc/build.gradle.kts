plugins {
    java
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.kikugie.dev/snapshots")
}
