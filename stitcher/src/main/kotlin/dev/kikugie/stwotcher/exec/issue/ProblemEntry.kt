package dev.kikugie.stwotcher.exec.issue

class ProblemEntry(val id: ProblemID, val range: IntRange) {
    var message: String = "No reason provided"
    var details: String? = null
    var solution: String? = null
    var documentation: String? = null
    var exception: Throwable? = null
    var severity: ProblemSeverity = ProblemSeverity.WARNING
}