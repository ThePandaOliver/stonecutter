# Project controller
## Project structure
The project settings step had split the mod into several subprojects
with the following structure:
```yaml
root:
  - src: {...}
  - versions:
    - 1.20.1: {...}
    - 1.21.1: {...}
    - 1.21.4: {...}
  - build.gradle[.kts] # Runs x3 (1.20.1, 1.21.1, 1.21.4)
  - stonecutter.gradle[.kts] # Runs x1 (root)
  - settings.gradle[.kts]
```

In a Stonecutter project, `stonecutter.gradle[.kts]` is referred to as a controller.
It's executed once, while `build.gradle[.kts]` runs for every registered version.

## Default controller
In a new project, Stonecutter will generate the following controller preset.
Note the `stonecutter.active "..."` line, as well as the `chiseledBuild` task below it -
these aspects are important during development with Stonecutter, and will be explained next.

> [!NOTE]
> If you're running Gradle tasks from a terminal, 
> you need to include the quotes: `./gradlew "Reset active version"`

::: tabs key:dsl
== stonecutter.gradle.kts
```kotlin
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.4"

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "project"
    ofTask("build")
}
```

== stonecutter.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
plugins {
    id "dev.kikugie.stonecutter"
}
stonecutter.active "1.21.4"

stonecutter.registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { 
    setGroup "project"
    ofTask "build"
}
```
:::

## Active version
Active version is an important part of the Stonecutter functionality.
While you develop your mod, the source code in the root `src/` directory
corresponds to one of the versions registered in [project settings](settings#specifying-versions).

The active version can be switched with `Switch active project to ...`
and `Reset active project` Gradle tasks, which are created in the `stonecutter` group.

::: tabs key:dsl
== stonecutter.gradle.kts
```kotlin {5}
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.4" // [!code focus]

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "project"
    ofTask("build")
}
```

== stonecutter.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy {5}
plugins {
    id "dev.kikugie.stonecutter"
}

stonecutter.active "1.21.4" // [!code focus]

stonecutter.registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { 
    setGroup "project"
    ofTask "build"
}
```
:::

> [!WARNING]
> Running `build` for inactive versions will result in an error, which is often one of the following:
> - `java.lang.IllegalArgumentException: Cannot nest jars into none mod jar {mod name}`;
> - `java.io.UncheckedIOException: java.io.IOException: Invalid paths argument, contained no existing paths: [...]`;
> - `java.nio.file.NoSuchFileException: Could not find AW '{accesswidener file}' to convert into AT!`.

## Chiseled tasks
To address the issue with building all versions of your mod, 
Stonecutter provides chiseled tasks.

::: tabs key:dsl
== stonecutter.gradle.kts
```kotlin {7-10}
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.4" 

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { // [!code focus:4]
    group = "project"
    ofTask("build")
}
```

== stonecutter.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy {7-10}
plugins {
    id "dev.kikugie.stonecutter"
}

stonecutter.active "1.21.4"

stonecutter.registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { // [!code focus:4]
    setGroup "project"
    ofTask "build"
}
```
:::

The default chiseled task is created in the `project` group,
and builds each version to `versions/{project}/build/libs`.

You can check out other common chiseled task uses in sections below.

::: details Chiseled build in one directory
### Build and collect
This task copies mod files into a single directory at `build/libs/{mod version}/`.

::: tabs key:dsl
== (stonecutter/build).gradle.kts
`build.gradle.kts`:
```kotlin
val modVersion = property("mod_version") as String

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/$modVersion"))
    dependsOn("build")
}
```
`stonecutter.gradle.kts`:
```kotlin
stonecutter registerChiseled tasks.register("chiseledBuildAndCollect", stonecutter.chiseled) {
    group = "project"
    ofTask("buildAndCollect")
}
```

== (stonecutter/build).gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.

`build.gradle`:
```groovy
def modVersion = project.mod_version

tasks.register("buildAndCollect", Copy) {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${modVersion}"))
    dependsOn("build")
}

```
`stonecutter.gradle`:
```groovy
stonecutter.registerChiseled tasks.register("chiseledBuildAndCollect", stonecutter.chiseled) {
    group = "project"
    ofTask("buildAndCollect")
}
```
:::

::: details Publish all with MPP
### Publish mods
This task publishes builds of all versions to Modrinth and Curseforge
using [Mod Publish Plugin](https://modmuss50.github.io/mod-publish-plugin/).

::: tabs key:dsl
== (stonecutter/build).gradle.kts
`build.gradle.kts`:
```kotlin
publishMods {
    val mod = property("mod_version") as String
    val mc = stonecutter.version.current
    version = "$mod+$mc"
    display = "My Mod $mod for $mc"
    // See MPP documentation for the complete configuration
}
```
`stonecutter.gradle.kts`:
```kotlin
stonecutter registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    group = "project"
    ofTask("publishMods")
}
```

== (stonecutter/build).gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
`build.gradle`:
```groovy
publishMods {
    def mod = project.mod_version
    def mc = stonecutter.version.current
    version = "${mod}+${mc}"
    display = "My Mod ${mod} for ${mc}"
    // See MPP documentation for the complete configuration
}
```
`stonecutter.gradle.kts`:
```groovy
stonecutter.registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    group = "project"
    ofTask("publishMods")
}
```
:::

::: details Build any version
### Build any
This task creates individual chiseled builds for each version,
avoiding the aforementioned issues, but with its own downsides.
> [!WARNING]
> This way of building the mod can make debugging more difficult,
> as IntelliJ won't be able to correctly resolve your mod's and Minecraft sources.

::: tabs key:dsl
== stonecutter.gradle.kts
```kotlin
for (meta in stonecutter.versions) {
    stonecutter registerChiseled tasks.register("build-${meta.project}", stonecutter.chiseled) {
        versions { _, it -> it == meta }
        group = "project"
        ofTask("build")
    }
}
```

== stonecutter.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
// TODO: To be added... eventually
```
:::