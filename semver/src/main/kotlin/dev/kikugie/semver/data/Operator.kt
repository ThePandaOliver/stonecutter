package dev.kikugie.semver.data

interface VersionOperator {
    val literal: String
    operator fun invoke(left: Version, right: Version): Boolean
}

object ImplicitEqualOperator : VersionOperator {
    override val literal: String = ""
    override fun invoke(left: Version, right: Version): Boolean = left.compareTo(right) == 0
}

enum class CommonOperator(override val literal: String) : VersionOperator {
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
    };
}

enum class SemanticOperator(override val literal: String) : VersionOperator {
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
}