# Mod setup
## Dependency notice
> [!IMPORTANT]
> The used modding toolkit versions may be outdated.
> Please check the official sources for the up-to-date releases.
> 
> The provided examples are made for a Fabric mod for writing convenience; 
> however, Stonecutter can be used with any mod loader.
> Please adjust the setup to your needs or join the [Discord server](https://discord.kikugie.dev)
> to get help from other users.

## Preparing properties
Some parameters used in `build.gradle[.kts]` will have to be different for each version.
The easiest way to do it is to specify them in `gradle.properties` files.

::: code-group
```properties [gradle.properties]
mod.id=template
mod.version=1.0.0

deps.fabric_loader=0.16.10

deps.yarn_mappings=[VERSIONED]
deps.fabric_api=[VERSIONED]
```
```properties [versions/1.20.1/gradle.properties]
deps.yarn_mappings=1.20.1+build.10
deps.fabric_api=0.92.1+1.20.1
```
```properties [versions/1.21.1/gradle.properties]
deps.yarn_mappings=1.21.1+build.3
deps.fabric_api=0.115.3+1.21.1
```
```properties [versions/1.21.1/gradle.properties]
deps.yarn_mappings=1.21.4+build.8
deps.fabric_api=0.119.2+1.21.4
```
:::

> [!NOTE]
> You can't use `libs.gradle.toml` for versioned dependencies. 
> Version catalogues don't support per-project entries, which makes them unusable.
> 
> You can use them for plugins and Minecraft-independent libraries, 
> but this guide will use the standard Gradle properties for consistency.

## Versioning build

::: tabs key:dsl
== build.gradle.kts
```kotlin
plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.project}")
    mappings("net.fabricmc:yarn:${property("deps.yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

tasks.processResources {
    inputs.property("minecraft", stonecutter.current.version)
    
    filesMatching("fabric.mod.json") { expand(mapOf(
        "minecraft" to stonecutter.current.version
    )) }
}

loom {
    runConfigs.all {
        ideConfigGenerated(true) // Run configurations are not created for subprojects by default
        runDir = "../../run" // Use a shared run folder and create separate worlds
    }
}
```

== build.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
plugins {
    id "fabric-loom" version "1.10-SNAPSHOT"
}

dependencies {
    minecraft "com.mojang:minecraft:${stonecutter.current.project}"
    mappings "net.fabricmc:yarn:${property('deps.yarn_mappings')}:v2"
    modImplementation "net.fabricmc:fabric-loader:${property('deps.fabric_loader')}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${property('deps.fabric_api')}"
}

processResources {
    inputs.property "minecraft", stonecutter.current.version

    filesMatching("fabric.mod.json") {
        expand "minecraft": stonecutter.current.version
    }
}

loom {
    runConfigs.all {
        ideConfigGenerated true // Run configurations are not created for subprojects by default
        runDir "../../run" // Use a shared run folder and create separate worlds
    }
}
```
:::

## Evaluating versions
Since the `build.gradle[.kts]` script executes for every declared version, 
you may want to adjust the setup based on the processed version.

A common use case is to adjust the target Java version between
Minecraft updates:
::: tabs key:dsl
== build.gradle.kts
```kotlin
java {
    withSourcesJar()
    val java = if (stonecutter.eval(stonecutter.version.current, ">=1.20.5"))
        JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}
```

== build.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
java {
    withSourcesJar()
    def java = stonecutter.eval(stonecutter.version.current, ">=1.20.5")
        ? JavaVersion.VERSION_21 : JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}
```
:::