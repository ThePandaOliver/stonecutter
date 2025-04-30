package dev.kikugie.stonecutter.data.dsl.impl

import dev.kikugie.semver.VersionParser
import dev.kikugie.stitcher.data.replacement.ReplacementList
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stonecutter.data.dsl.ConstantContainer
import dev.kikugie.stonecutter.data.dsl.DependencyContainer
import dev.kikugie.stonecutter.data.dsl.FilterContainer
import dev.kikugie.stonecutter.data.dsl.ReplacementContainer
import dev.kikugie.stonecutter.data.dsl.SwapContainer
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.isIdentifier
import dev.kikugie.stonecutter.util.newInstance
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import java.nio.file.Path
import java.util.function.IntFunction
import kotlin.io.path.extension
import kotlin.text.contains
import kotlin.to

@Suppress("NOTHING_TO_INLINE")
private inline fun checkKeyImpl(key: String, type: String) {
    require(key.isNotBlank()) { "$type key '$key' must not be blank" }
    require(isIdentifier(key)) { "$type key '$key' must be a valid identifier" }
}

internal class ConstantContainerImpl : CheckedMutableMap<String, Boolean>(), ConstantContainer {
    override fun checkKey(key: String) = checkKeyImpl(key, "Constant")
}

internal class SwapContainerImpl : CheckedMutableMap<String, String>(), SwapContainer {
    override fun checkKey(key: String) = checkKeyImpl(key, "Swap")
}

internal class DependencyContainerImpl : CheckedMutableMap<String, String>(), DependencyContainer {
    override fun checkKey(key: String) = checkKeyImpl(key, "Dependency")
    override fun checkValue(value: String) {
        require(value.isNotBlank()) { "Dependency value must not be blank" }
        require(isIdentifier(value) || kotlin.runCatching {
            VersionParser.parseLenient(value, full = true)
        }.isSuccess) { "Dependency value '$value' must be a valid identifier or a valid semantic version." }
    }
}

internal class FilterContainerImpl : FilterContainer {
    override val extensions: FilterContainer.AllowedExtensions = AllowedExtensionsImpl()
    override val excludes: FilterContainer.ExcludedFiles = ExcludedFilesImpl()

    class AllowedExtensionsImpl : CheckedMutableSet<String>(), FilterContainer.AllowedExtensions {
        override fun checkElement(element: String) {
            require(element.isNotBlank()) { "Extension  must not be blank" }
            require(element.all(Char::isLetterOrDigit)) { "Extension '$element' must be alphanumeric" }
        }
    }

    class ExcludedFilesImpl : CheckedMutableSet<String>(), FilterContainer.ExcludedFiles {
        override fun checkElement(element: String) {
            require(element.startsWith("src/")) { "Filtered files must be in the src/ directory" }
        }
    }

    fun filter(file: Path) = file.extension in extensions && excludes.none { file.endsWith(it) }
}

internal class ReplacementContainerImpl(val objects: ObjectFactory) : ReplacementContainer {
    val replacements = ReplacementList()
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