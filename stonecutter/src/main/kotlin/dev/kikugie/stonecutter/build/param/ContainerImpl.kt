package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.VersionParser
import dev.kikugie.stitcher.data.replacement.ReplacementList
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.build.dsl.*
import dev.kikugie.stonecutter.build.dsl.ReplacementContainer.RegexReplacementBuilder
import dev.kikugie.stonecutter.build.dsl.ReplacementContainer.StringReplacementBuilder
import dev.kikugie.stonecutter.data.build.newParameterMap
import dev.kikugie.stonecutter.then
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.isIdentifier
import dev.kikugie.stonecutter.util.newInstance
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import java.nio.file.Path
import kotlin.io.path.extension

@Suppress("NOTHING_TO_INLINE")
private inline fun checkKey(key: String, type: String) {
    require(key.isNotBlank()) { "$type key '$key' must not be blank." }
    require(isIdentifier(key)) { "$type key '$key' must be a valid identifier." }
}

//private fun unpackPredicates(value: CharSequence, matcher: VersionOperations): List<VersionPredicate> = buildList {
//    var offset = 0
//    while (offset < value.length) when {
//        value[offset].isWhitespace() -> offset++
//        else -> {
//            val boundary = matcher.getPredicateBoundary(value, offset)
//            if (boundary == offset) throw VersionParsingException("Invalid predicate", offset..boundary)
//            this += matcher.parsePredicate(value.subSequence(offset, boundary))
//                .formatParsingException(value, offset).getOrThrow()
//            offset = boundary
//        }
//    }
//}

internal class ConstantContainerImpl(val delegate: MutableMap<Identifier, Boolean>) : ConstantContainer, MutableMap<Identifier, Boolean> by delegate {
    constructor() : this(
        newParameterMap(
            keyCheck = { checkKey(it, "Constant") }
        ))
}

internal class SwapContainerImpl(val delegate: MutableMap<Identifier, String>) : SwapContainer, MutableMap<Identifier, String> by delegate {
    constructor() : this(
        newParameterMap(
            keyCheck = { checkKey(it, "Swap") }
        ))
}

internal class DependencyContainerImpl(val delegate: MutableMap<Identifier, Version>) : DependencyContainer,
    MutableMap<Identifier, Version> by delegate {
    constructor() : this(
        newParameterMap(
            keyCheck = { checkKey(it, "Dependency") },
            valueCheck = {
                require(it.isNotBlank()) { "Dependency value '$it' must not be blank." }
                require(isIdentifier(it) || kotlin.runCatching { VersionParser.parseLenient(it, full = true) }.isSuccess)
            }
        ))
}

internal class FilterContainerImpl(
    override var extensions: FilterContainer.AllowedExtensions = AllowedExtensionsImpl(),
    override var excludes: FilterContainer.ExcludedFiles = ExcludedFilesImpl()
) : FilterContainer {
    class AllowedExtensionsImpl(val delegate: MutableSet<String> = mutableSetOf()) : FilterContainer.AllowedExtensions, MutableSet<String> by delegate {
        override fun add(element: String): Boolean = checkExtension(element) then delegate.add(element)
        override fun addAll(elements: Collection<String>): Boolean = elements.forEach(::checkExtension) then delegate.addAll(elements)

        private fun checkExtension(extension: String) {
            require(extension.isNotBlank()) { "Extension '$extension' must not be blank." }
            require(extension.all(Char::isLetterOrDigit)) { "Extension '$extension' must be alphanumeric." }
        }
    }

    class ExcludedFilesImpl(val delegate: MutableSet<String> = mutableSetOf()) : FilterContainer.ExcludedFiles, MutableSet<String> by delegate {
        override fun add(element: String): Boolean = checkPath(element) then delegate.add(element)
        override fun addAll(elements: Collection<String>): Boolean = elements.forEach(::checkPath) then delegate.addAll(elements)

        private fun checkPath(path: String) {
            require(path.startsWith("src/")) { "Filtered files must be in the src/ directory" }
        }
    }

    fun filter(file: Path) = file.extension in extensions
        && excludes.none { file.endsWith(it) }
}

internal class ReplacementContainerImpl(val objects: ObjectFactory, val delegate: ReplacementList = ReplacementList()) : ReplacementContainer {
    override fun string(block: Action<StringReplacementBuilder>) =
        objects.newInstance<StringReplacementBuilder> { block.execute(this) }.build()

    override fun regex(block: Action<RegexReplacementBuilder>) =
        objects.newInstance<RegexReplacementBuilder> { block.execute(this) }.build()

    private fun StringReplacementBuilder.build() {
        require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
        val realPhase = ReplacementPhase.valueOf(phase().uppercase())
        if (direction()) delegate.addString(from(), to(), realPhase, id.orNull)
        else delegate.addString(to(), from(), realPhase, id.orNull)
    }

    private fun RegexReplacementBuilder.build() {
        require(!id.isPresent || isIdentifier(id())) { "Invalid identifier: '${id()}'" }
        val realPhase = ReplacementPhase.valueOf(phase().uppercase())
        if (direction()) delegate.addRegex(fromPattern().toRegex(), toValue(), realPhase, id.orNull)
        else delegate.addRegex(reversePattern().toRegex(), reverseValue(), realPhase, id.orNull)
    }
}

//internal object SemanticVersionProvider : VersionProvider<SemanticVersion> {
//    override fun parseVersion(value: CharSequence): Result<SemanticVersion> = SemanticVersionOperations.parseVersion(value).formatParsingException(value)
//    override fun parsePredicate(value: CharSequence): Result<VersionPredicate> = SemanticVersionOperations.parsePredicate(value).formatParsingException(value)
//    override fun eval(target: LenientVersion, vararg predicates: CharSequence): Boolean =
//        predicates.flatMap { unpackPredicates(it, SemanticVersionOperations) }.all { it(target) }
//}
//
//internal object LenientVersionProvider : VersionProvider<LenientVersion> {
//    override fun parseVersion(value: CharSequence): Result<LenientVersion> = LenientVersionOperations.parseVersion(value).formatParsingException(value)
//    override fun parsePredicate(value: CharSequence): Result<VersionPredicate> = LenientVersionOperations.parsePredicate(value).formatParsingException(value)
//    override fun eval(target: LenientVersion, vararg predicates: CharSequence): Boolean =
//        predicates.flatMap { unpackPredicates(it, LenientVersionOperations) }.all { it(target) }
//}