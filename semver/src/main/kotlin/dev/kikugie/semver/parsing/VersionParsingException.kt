package dev.kikugie.semver.parsing

class VersionParsingException(message: String, val range: IntRange) : RuntimeException(message) {
    fun formatted(source: CharSequence, offset: Int): VersionParsingException {
        // FIXME: this shit is ass
        val range = (range.first + offset).coerceAtLeast(0)..(range.last + offset).coerceAtMost(source.lastIndex)
        val decorated = StringBuilder(source).apply {
            removeRange(range)
            insert(range.first, " -{${source.substring(range)}}- ")
            insert(0, "$message: ")
        }
        return VersionParsingException(decorated.toString(), range).apply {
            stackTrace = this@VersionParsingException.stackTrace
            this@VersionParsingException.suppressed.forEach<Throwable>(::addSuppressed)
            this@VersionParsingException.cause?.let(::initCause)
        }
    }
}