package dev.kikugie.stonecutter.data.dsl.impl

import dev.kikugie.commons.text.countWhile
import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.VersionPredicate
import dev.kikugie.semver.impl.VersionParsingException
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import java.util.concurrent.ConcurrentHashMap

private val LENIENT_CACHE: MutableMap<String, ParsedVersion> = ConcurrentHashMap()
private val SEMANTIC_CACHE: MutableMap<String, ParsedVersion> = ConcurrentHashMap()

private fun VersionParsingException.present(version: String) = buildString {
    appendLine("$message:")
    appendLine(version)
    append(" ".repeat(position) + "^")
}

private fun doParse(version: String, parser: ParsedVersion.Operations): ParsedVersion = parser.parse(version)
    .getOrElse { it as VersionParsingException; throw VersionParsingException(it.present(version), it.position) }

internal fun checkCaching(cache: MutableMap<String, ParsedVersion>, version: String, parser: ParsedVersion.Operations): Boolean =
    runCatching { getOrParse<ParsedVersion>(cache, version, parser) }.isSuccess

@Suppress("UNCHECKED_CAST")
internal fun <T : ParsedVersion> getOrParse(cache: MutableMap<String, ParsedVersion>, version: String, parser: ParsedVersion.Operations): T =
    cache.computeIfAbsent(version) { doParse(it, parser) } as T

private fun unpackPredicates(source: String, operations: VersionPredicate.Operations): List<VersionPredicate> = buildList {
    var cursor = 0
    while (cursor < source.length) {
        cursor += source.countWhile(cursor, predicate = Char::isWhitespace)
        if (cursor >= source.length) break
        cursor = operations.locate(source, cursor).also {
            if (it < 0) throw IllegalArgumentException("Unable to locate predicate at: '${source.substring(cursor)}'")
            val predicate = source.substring(cursor, it)
            this += operations.parse(predicate).getOrElse {
                it as VersionParsingException
                throw VersionParsingException(it.present(predicate), it.position)
            }
        }
    }
}

private fun VersionPredicate.Operations.eval(version: ParsedVersion, vararg predicates: String): Boolean =
    predicates.flatMap { unpackPredicates(it, this) }.all { it(version) }

internal object SemanticOperations : VersionOperations<SemanticVersion> {
    override fun check(version: String): Boolean = checkCaching(SEMANTIC_CACHE, version, SemanticVersion)
    override fun parse(version: String): SemanticVersion = getOrParse(SEMANTIC_CACHE, version, SemanticVersion)
    override fun eval(version: ParsedVersion, vararg predicates: String): Boolean =
        VersionPredicate.Semantic.eval(version, *predicates)
}

internal object LenientOperations : VersionOperations<ParsedVersion> {
    override fun check(version: String): Boolean = checkCaching(LENIENT_CACHE, version, ParsedVersion)
    override fun parse(version: String): ParsedVersion = getOrParse(LENIENT_CACHE, version, ParsedVersion)
    override fun eval(version: ParsedVersion, vararg predicates: String): Boolean =
        VersionPredicate.eval(version, *predicates)
}
