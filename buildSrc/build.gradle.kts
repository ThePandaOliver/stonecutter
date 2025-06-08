plugins {
    java
    `kotlin-dsl`
    alias(common.plugins.kotlin.jvm)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.kikugie.dev/snapshots")
}
