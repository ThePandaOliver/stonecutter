package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.data.token.ContentType
import dev.kikugie.stitcher.scanner.CommentRecognizer
import dev.kikugie.stitcher.scanner.Scanner
import dev.kikugie.stitcher.transformer.getOrSpace
import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language

@Serializable
sealed interface Replacement {
    val phase: Phase
    val identifier: String?


    @Serializable
    data class StringReplacement(
        val sources: MutableSet<String>,
        var target: String,
        override val phase: Phase = Phase.FIRST,
        override val identifier: String? = null
    ) : Replacement {
        constructor(source: String, target: String, phase: Phase = Phase.FIRST, identifier: String? = null)
                : this(mutableSetOf(source), target, phase, identifier)

        override fun toString(): String = buildString {
            append("StringReplacement(")
            if (identifier != null) append("$identifier, ")
            append("$phase) [${sources.joinToString { "'$it'" }} -> '$target']")
        }
    }

    class RegexReplacement(
        val pattern: Regex,
        val target: String,
        override val phase: Phase,
        override val identifier: String?
    ) : Replacement {
        override fun toString(): String = buildString {
            append("RegexReplacement(")
            if (identifier != null) append("$identifier, ")
            append("$phase) [${pattern.pattern} -> '$target']")
        }
    }


    @Serializable
    /**Represents the stage a replacement is executed.*/
    enum class Phase {
        /**Replaces values before the contents are parsed.*/
        FIRST,

        /**Replaces values after versioned comments have been evaluated and reassembled.*/
        LAST;
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        const val PREFIX = '~'

        fun CharSequence.getReplacementTokens(recognizers: Iterable<CommentRecognizer>): Set<String> = buildSet {
            for (token in Scanner(this@getReplacementTokens, recognizers)) when (token.type as ContentType) {
                ContentType.COMMENT_START, ContentType.COMMENT_END -> continue
                ContentType.CONTENT -> if (token.value.isNotBlank()) break
                ContentType.COMMENT -> if (token.value.getOrSpace(0) == PREFIX)
                    this += token.value.substring(1).trim()
            }
        }

        fun Collection<Replacement>.replace(text: CharSequence, phase: Phase, tokens: Set<String> = emptySet()): CharSequence {
            val matching = filter { it.phase == phase && (it.identifier == null || it.identifier in tokens) }
                .also { if (it.isEmpty()) return text }

            return StringBuilder(text)
                .replaceString(matching.mergeStringReplacements())
                .replaceRegex(matching.mergeRegexReplacements())
        }

        private fun Collection<Replacement>.mergeStringReplacements(): Sequence<StringReplacement> {
            if (isEmpty()) return emptySequence()

            val unassigned: MutableList<StringReplacement>
            val named: List<StringReplacement>
            filterIsInstance<StringReplacement>().groupBy { it.identifier != null }.let {
                unassigned = it[false]?.toMutableList() ?: mutableListOf()
                named = it[true] ?: emptyList()
            }

            for (repl in named) for (src in repl.sources) (unassigned as MutableList<Replacement>)
                .string(src, repl.target, repl.phase)
            return unassigned.asSequence()
        }

        private fun Collection<Replacement>.mergeRegexReplacements() = if (isEmpty()) emptySequence() else
            filterIsInstance<RegexReplacement>().asSequence()

        private inline fun <reified T : Replacement> Replacement.check(phase: Phase, checker: (String?) -> Boolean) =
            this is T && this.phase == phase && checker(identifier)

        private fun StringBuilder.replaceString(entries: Sequence<StringReplacement>): StringBuilder {
            val lookup = entries.flatMap { it.sources.map { s -> s to it.target } }.toMap()
            for ((key, value) in lookup) {
                var index = indexOf(key)
                while (index >= 0) {
                    replace(index, index + key.length, value)
                    index += value.length
                    index = indexOf(key, index)
                }
            }
            return this
        }

        private fun StringBuilder.replaceRegex(entries: Sequence<RegexReplacement>): StringBuilder {
            for (repl in entries) repl.pattern.replace(this, repl.target).let {
                replace(0, length, it)
            }
            return this
        }

        fun MutableCollection<Replacement>.regex(
            @Language("RegExp") pattern: String,
            to: String,
            phase: Phase = Phase.LAST,
            identifier: String? = null
        ) {
            require(pattern.isNotEmpty()) { "Can't replace empty pattern" }
            if (isEmpty()) this += RegexReplacement(pattern.toRegex(), to, phase, identifier) else when {
                identifier != null -> {
                    val match = find { it.identifier == identifier }
                    require(match == null) { "Replacement '$identifier' is already registered for $match" }
                    this += RegexReplacement(pattern.toRegex(), to, phase, identifier)
                }

                else -> {
                    this += RegexReplacement(pattern.toRegex(), to, phase, null)
                }
            }
        }

        fun MutableCollection<Replacement>.string(
            from: String,
            to: String,
            phase: Phase = Phase.LAST,
            identifier: String? = null
        ) {
            require(from.isNotEmpty()) { "Can't replace empty string" }
            if (isEmpty()) this += StringReplacement(from, to, phase, identifier) else when {
                identifier != null -> {
                    val match = find { it.identifier == identifier }
                    if (match == null) this += StringReplacement(from, to, phase, identifier) else {
                        require(match is StringReplacement) { "Replacement '$identifier' is already registered for $match" }
                        require(match.phase == phase) { "Replacement '$identifier' is already registered for $match with a different phase" }
                        require(match.tryMerge(from, to)) { "Replacement '$from' -> '$to' couldn't be merged with $match" }
                    }
                }

                else -> {
                    var merged = false
                    for (it in this) when {
                        it.identifier != null || it.phase != phase -> continue
                        it is StringReplacement && it.tryMerge(from, to) -> {
                            merged = true; break
                        }
                    }
                    if (!merged) this += StringReplacement(from, to, phase)
                    else Unit
                }
            }

        }

        private fun StringReplacement.tryMerge(from: String, to: String): Boolean = when {
            from == target -> {
                require(to !in sources) { "Replacement '$from' -> '$to' forms a cycle with $this" }
                target = to; sources += from; true
            }

            to == target || to in sources -> {
                sources += from; true
            }

            from in sources -> throw IllegalArgumentException("Replacement '$to' can't be replaced by both '$from' and $this")
            else -> false
        }
    }
}