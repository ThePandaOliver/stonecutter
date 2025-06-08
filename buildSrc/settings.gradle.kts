dependencyResolutionManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }

    versionCatalogs {
        create("common") { from("dev.kikugie:stonecutter-versions:1-SNAPSHOT") }
    }
}