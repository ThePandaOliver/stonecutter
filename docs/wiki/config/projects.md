# Data-driven projects
## Prerequisites
This article is an extension to the [project settings](../start/settings) 
and [project branches] (TBA) pages and will reference terms from them,
expecting you to be familiar with those concepts.

Versioned projects are defined in the `settings.gradle[.kts]` file,
however, when using third-party tools alongside Stonecutter, you may want
to know or determine the project structure dynamically based on your data.

The data-driven setup allows reading project settings from a JSON *(or JSON5 with limited syntax)* file.

## JSON format
When writing the project setup JSON file, you can use the
[JSON schema](https://github.com/stonecutter-versioning/stonecutter/blob/0.6/tools/settings-schema.json)
as a guideline. For more detailed examples of each format, use the collapsed sections below.

::: details Plain list
### Plain list
```json5
{
  "$schema": "https://github.com/stonecutter-versioning/stonecutter/blob/0.6/tools/settings-schema.json",
  "vcs": "1.21.1",
  "versions": [
    "1.20.1", "1.21.1", "1.21.4"
  ]
}
```
:::
::: details Branch to versions
### Branch to versions
```json5
{
  "$schema": "https://github.com/stonecutter-versioning/stonecutter/blob/0.6/tools/settings-schema.json",
  "vcs": "1.21.1",
  "branches": {
    "": [
      "1.20.1", "1.21.1", "1.21.4"
    ],
    "subproject": [
      // ...
    ]
  }
}
```
:::
::: details Version to branches
### Version to branches
```json5
{
  "$schema": "https://github.com/stonecutter-versioning/stonecutter/blob/0.6/tools/settings-schema.json",
  "vcs": "1.21.1",
  "versions": {
    "1.21.1": [
      "", "fabric", "neoforge"
    ],
    "1.21.4": [
      "", "fabric"
    ]
  }
}
```
:::

## Applying settings
::: tabs key:dsl
== settings.gradle.kts
```kotlin {2}
stonecutter {
    create(rootProject, file("versions.json"))
}
```

== settings.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts is limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.

```groovy {2}
stonecutter {
    create(getRootProject(), file("versions.json"))
}
```
:::

## Active version
Must have the version on the first line.
*More info is TBA, figure it out*
::: tabs key:dsl
== stonecutter.gradle.kts
```kotlin {5}
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active file("active_version.txt")
```

== stonecutter.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy {5}
plugins {
    id "dev.kikugie.stonecutter"
}

stonecutter.active file("active_version.txt")
```
:::