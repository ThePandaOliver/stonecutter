package dev.kikugie.stonecutter.data.dsl.impl

import dev.kikugie.commons.text.countWhile
import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.VersionPredicate
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import java.util.concurrent.ConcurrentHashMap

private val VERSION_CACHE: MutableMap<String, ParsedVersion> = ConcurrentHashMap()

@Suppress("UNCHECKED_CAST")
internal fun <T : ParsedVersion> getOrParse(version: String, parser: ParsedVersion.Operations): T =
    VERSION_CACHE.computeIfAbsent(version) { parser.parse(it).getOrThrow() } as T

private fun unpackPredicates(source: String, operations: VersionPredicate.Operations): List<VersionPredicate> = buildList {
    var cursor = 0
    while (cursor < source.length) {
        cursor += source.countWhile(cursor, predicate = Char::isWhitespace)
        if (cursor >= source.length) break
        cursor = operations.locate(source, cursor).also {
            if (it < 0) throw IllegalArgumentException("Unable to locate predicate at: '${source.substring(cursor)}'")
            val predicate = source.substring(cursor, it)
            this += operations.parse(predicate)
                .recoverCatching { throw IllegalArgumentException("Invalid predicate: '$predicate'").initCause(it) }
                .getOrThrow()
        }
    }
}

private fun VersionPredicate.Operations.eval(version: ParsedVersion, vararg predicates: String): Boolean =
    predicates.flatMap { unpackPredicates(it, this) }.all { it(version) }

internal object SemanticOperations : VersionOperations<SemanticVersion> {
    override fun parse(version: String): SemanticVersion = getOrParse(version, SemanticVersion)
    override fun eval(version: ParsedVersion, vararg predicates: String): Boolean =
        VersionPredicate.Semantic.eval(version, *predicates)
}

internal object LenientOperations : VersionOperations<ParsedVersion> {
    override fun parse(version: String): ParsedVersion = getOrParse(version, ParsedVersion)
    override fun eval(version: ParsedVersion, vararg predicates: String): Boolean =
        VersionPredicate.eval(version, *predicates)
}
