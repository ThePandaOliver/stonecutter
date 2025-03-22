package dev.kikugie.stwotcher.exec.issue

data class ProblemID(val id: String, val display: String) {
    init {
        require(id.isNotBlank()) { "ID cannot be blank" }
        require(id.isKebabCase()) { "ID must be written in kebab-case" }
        require(display.isNotBlank()) { "Display cannot be blank" }
    }

    companion object {
        private fun String.isKebabCase() = all { it == '-' || it.isLetterOrDigit() }

        val UNEXPECTED_EXPRESSION = ProblemID("unexpected-expression", "Unexpected expression")
        val MISSING_PARAMETER = ProblemID("missing-parameter", "Missing parameter")
        val INVALID_CLOSER = ProblemID("invalid-closer", "Invalid closer")
        val INVALID_REFERENCE = ProblemID("invalid-reference", "Invalid reference")
    }
}