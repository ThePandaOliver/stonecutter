package dev.kikugie.semver.data

enum class VersionOperator(val literal: String) {
    IMPLICIT_EQUAL("") {
        override fun invoke(left: Version, right: Version) = left.compareTo(right) == 0
    },
    EQUAL("=") {
        override fun invoke(left: Version, right: Version) = left.compareTo(right) == 0
    },
    LESS("<") {
        override fun invoke(left: Version, right: Version) = left < right
    },
    GREATER(">") {
        override fun invoke(left: Version, right: Version) = left > right
    },
    LESS_EQUAL("<=") {
        override fun invoke(left: Version, right: Version) = left <= right
    },
    GREATER_EQUAL(">=") {
        override fun invoke(left: Version, right: Version) = left >= right
    },
    SAME_MINOR("~") {
        override fun invoke(left: Version, right: Version) = left is SemanticVersion && right is SemanticVersion
            && left >= right
            && left.components[0] == right.components[0]
            && left.components[1] == right.components[1]
    },
    SAME_MAJOR("^") {
        override fun invoke(left: Version, right: Version) = left is SemanticVersion && right is SemanticVersion
            && left >= right
            && left.components[0] == right.components[0]
    };

    abstract operator fun invoke(left: Version, right: Version): Boolean
}