package dev.kikugie.stonecutter.data.dsl.impl

import dev.kikugie.semver.Version
import dev.kikugie.stitcher.data.replacement.ReplacementList
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stonecutter.data.dsl.*
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.isIdentifier
import dev.kikugie.stonecutter.util.newInstance
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import java.nio.file.Path
import kotlin.io.path.extension

@Suppress("NOTHING_TO_INLINE")
private inline fun checkKeyImpl(key: String, type: String) {
    require(key.isNotBlank()) { "$type key '$key' must not be blank" }
    require(isIdentifier(key)) { "$type key '$key' must be a valid identifier" }
}

internal class ConstantContainerImpl(delegate: MutableMap<String, Boolean>) : CheckedMutableMap<String, Boolean>(delegate), ConstantContainer {
    override fun checkKey(key: String) = checkKeyImpl(key, "Constant")
}

internal class SwapContainerImpl(delegate: MutableMap<String, String>) : CheckedMutableMap<String, String>(delegate), SwapContainer {
    override fun checkKey(key: String) = checkKeyImpl(key, "Swap")
}

internal class DependencyContainerImpl(delegate: MutableMap<String, Version>) : CheckedMutableMap<String, Version>(delegate), DependencyContainer {
    override fun checkKey(key: String) = checkKeyImpl(key, "Dependency")
}

internal class FilterContainerImpl(extensions: MutableSet<String>, excludes: MutableSet<String>) : FilterContainer {
    override val extensions: FilterContainer.AllowedExtensions = AllowedExtensionsImpl(extensions)
    override val excludes: FilterContainer.ExcludedFiles = ExcludedFilesImpl(excludes)

    class AllowedExtensionsImpl(delegate: MutableSet<String>) : CheckedMutableSet<String>(delegate), FilterContainer.AllowedExtensions {
        override fun checkElement(element: String) {
            require(element.isNotBlank()) { "Extension  must not be blank" }
            require(element.all(Char::isLetterOrDigit)) { "Extension '$element' must be alphanumeric" }
        }
    }

    class ExcludedFilesImpl(delegate: MutableSet<String>) : CheckedMutableSet<String>(delegate), FilterContainer.ExcludedFiles {
        override fun checkElement(element: String) {
            require(element.startsWith("src/")) { "Filtered files must be in the src/ directory" }
        }
    }

    fun filter(file: Path) = file.extension in extensions && excludes.none { file.endsWith(it) }
}

internal class ReplacementContainerImpl(val replacements: ReplacementList, val objects: ObjectFactory) : ReplacementContainer {
    override fun string(block: Action<ReplacementContainer.StringReplacementBuilder>) =
        objects.newInstance<ReplacementContainer.StringReplacementBuilder> { block.execute(this) }.build()

    override fun regex(block: Action<ReplacementContainer.RegexReplacementBuilder>) =
        objects.newInstance<ReplacementContainer.RegexReplacementBuilder> { block.execute(this) }.build()


    private fun ReplacementContainer.StringReplacementBuilder.build() {
        require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
        val phase = ReplacementPhase.valueOf(phase().uppercase())
        if (direction()) replacements.addString(from(), to(), phase, id.orNull)
        else replacements.addString(to(), from(), phase, id.orNull)
    }

    private fun ReplacementContainer.RegexReplacementBuilder.build() {
        require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
        val phase = ReplacementPhase.valueOf(phase().uppercase())
        if (direction()) replacements.addRegex(fromPattern().toRegex(), toValue(), phase, id.orNull)
        else replacements.addRegex(reversePattern().toRegex(), reverseValue(), phase, id.orNull)
    }
}