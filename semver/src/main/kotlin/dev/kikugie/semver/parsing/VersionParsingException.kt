package dev.kikugie.semver.parsing

class VersionParsingException(message: String, val range: IntRange)
    : RuntimeException(message)