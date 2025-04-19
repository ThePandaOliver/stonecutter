package dev.kikugie.semver.data

data class VersionPredicate(
    val operator: VersionOperator,
    val version: Version
) {
    operator fun invoke(target: Version): Boolean =
        operator(target, version)
}
