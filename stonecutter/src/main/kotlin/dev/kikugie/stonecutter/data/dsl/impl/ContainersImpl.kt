package dev.kikugie.stonecutter.data.dsl.impl

import dev.kikugie.stitcher.data.replacement.Replacement
import dev.kikugie.stitcher.data.replacement.ReplacementList
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.data.dsl.ConstantContainer
import dev.kikugie.stonecutter.data.dsl.DependencyContainer
import dev.kikugie.stonecutter.data.dsl.ReplacementContainer
import dev.kikugie.stonecutter.data.dsl.SwapContainer
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.isIdentifier
import dev.kikugie.stonecutter.util.newInstance
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ProviderFactory

private fun checkKeyImpl(key: Identifier, type: String) {
    require(key.isNotBlank()) { "$type key '$key' must not be blank" }
    require(isIdentifier(key)) { "$type key '$key' must be a valid identifier" }
}

internal class ConstantContainerImpl(factory: ProviderFactory, property: MapProperty<Identifier, Boolean>) :
    PropertyBackedMap<Identifier, Boolean>(factory, property), ConstantContainer {
    override fun checkKey(key: Identifier) = checkKeyImpl(key, "Constant")
}

internal class SwapContainerImpl(factory: ProviderFactory, property: MapProperty<Identifier, String>) :
    PropertyBackedMap<Identifier, String>(factory, property), SwapContainer {
    override fun checkKey(key: Identifier) = checkKeyImpl(key, "Swap")
}

internal class DependencyContainerImpl(factory: ProviderFactory, property: MapProperty<Identifier, Version>) :
    PropertyBackedMap<Identifier, Version>(factory, property), DependencyContainer {
    override fun checkKey(key: Identifier) {
        if (key.isNotEmpty()) checkKeyImpl(key, "Dependency")
    }
    override fun checkValue(value: Version) { LenientOperations.parse(value) }
}

internal class ReplacementContainerImpl(private val objects: ObjectFactory, internal val list: MutableList<Replacement>) : ReplacementContainer {
    private val replacements: ReplacementList inline get() = ReplacementList(list)

    override fun string(action: Action<ReplacementContainer.StringReplacementBuilder>) =
        objects.newInstance<ReplacementContainer.StringReplacementBuilder> { action.execute(this) }.build()

    override fun regex(action: Action<ReplacementContainer.RegexReplacementBuilder>) =
        objects.newInstance<ReplacementContainer.RegexReplacementBuilder> { action.execute(this) }.build()

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
