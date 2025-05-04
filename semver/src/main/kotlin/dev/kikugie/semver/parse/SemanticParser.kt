package dev.kikugie.semver.parse

import dev.kikugie.semver.data.SemanticVersion

@JvmInline
value class SemanticParser(val input: String) {
    fun parse(): Result<SemanticVersion> = kotlin.runCatching {
        require(input.isNotEmpty()) { "Version string cannot be empty" }
        var cursor = 0

        val components = mutableListOf<Int>().also {
            cursor = parseHead(it)
        }
        val preRelease = parsePreRelease(cursor).let {
            if (it == cursor) return@let ""
            input.substring(cursor + 1, it).apply { cursor = it }
        }
        val buildMetadata = parseBuildMetadata(cursor).let {
            if (it == cursor) return@let ""
            input.substring(cursor + 1, it).apply { cursor = it }
        }

        require(cursor == input.length) { "Version input string has not been fully consumed" }
        SemanticVersion(components.toIntArray(), preRelease, buildMetadata)
    }

    private fun parseHead(collector: MutableList<Int>): Int {
        var cursor = 0
        var number = -1
        for (char in input) when(char) {
            in '0'..'9' -> {
                number = if (number < 0) char - '0' else number * 10 + (char - '0')
                cursor++
            }
            '.' -> {
                check(number >= 0) { "Invalid version component" }
                collector += number; number = -1; cursor++
            }
            else -> break
        }
        require(number >= 0) { "Invalid version component" }
        collector += number
        return cursor
    }

    private fun parsePreRelease(start: Int): Int {
        if (start >= input.length || input[start] != '-') return start
        var cursor = start + 1

        require(cursor < input.length) { "Empty pre-release modifier" }
        while (cursor < input.length) when (input[cursor]) {
            in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.' -> cursor++
            '+' -> if (cursor != start + 1) break
            else throw IllegalArgumentException("Empty pre-release modifier")
            else -> throw IllegalArgumentException("Invalid pre-release modifier")
        }
        return cursor
    }

    private fun parseBuildMetadata(start: Int): Int {
        if (start >= input.length || input[start] != '+') return start
        var cursor = start + 1

        require(cursor < input.length) { "Empty build metadata" }
        while (cursor < input.length) when (input[cursor]) {
            in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.' -> cursor++
            else -> throw IllegalArgumentException("Invalid build metadata")
        }
        return cursor
    }
}