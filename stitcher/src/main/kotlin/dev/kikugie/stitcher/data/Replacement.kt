package dev.kikugie.stitcher.data

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
            append("$phase) [${sources.joinToString { "'$it'" }} -> '$target']" )
        }
    }

    class RegexReplacement(
        val target: String,
        val pattern: Regex,
        override val phase: Phase,
        override val identifier: String?
    ) : Replacement {
        override fun toString(): String = buildString {
            append("RegexReplacement(")
            if (identifier != null) append("$identifier, ")
            append("$phase) [${pattern.pattern} -> '$target']" )
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

    companion object {
        fun Collection<Replacement>.replace(text: CharSequence, phase: Phase, tokens: Set<String> = emptySet()): CharSequence = StringBuilder(text)
            .replaceString(filterForReplacement(phase, tokens))
            .replaceRegex(filterForReplacement(phase, tokens))

        @Suppress("UNCHECKED_CAST")
        private inline fun <reified T : Replacement> Collection<Replacement>.filterForReplacement(phase: Phase, tokens: Set<String>) = asSequence()
            .filter { it is T && it.phase == phase && (it.identifier == null || it.identifier in tokens) } as Sequence<T>

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
            phase: Phase = Phase.FIRST,
            identifier: String? = null
        ) = when {
            identifier != null -> {
                val match = find { it.identifier == identifier }
                require(match == null) { "Replacement '$identifier' is already registered for $match" }
                this += RegexReplacement(to, pattern.toRegex(), phase, identifier)
            }

            else -> {
                this += RegexReplacement(to, pattern.toRegex(), phase, null)
            }
        }

        fun MutableCollection<Replacement>.string(
            from: String,
            to: String,
            phase: Phase = Phase.FIRST,
            identifier: String? = null
        ) = when {
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