dependencyResolutionManagement {
    versionCatalogs {
        create("buildlibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}