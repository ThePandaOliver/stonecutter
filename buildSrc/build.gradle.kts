plugins {
    java
    `kotlin-dsl`
    alias(buildlibs.plugins.kotlin.jvm)
    alias(buildlibs.plugins.kotlin.serialization)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.kikugie.dev/snapshots")
}

dependencies {
    implementation(buildlibs.bundles.buildsrc)
}