package dev.kikugie.stonecutter.build.param

import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stonecutter.data.dsl.FilterContainer
import dev.kikugie.stonecutter.data.dsl.ReplacementContainer
import dev.kikugie.stonecutter.data.dsl.SwapContainer
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.ConstantContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.DependencyContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.FilterContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.dsl.impl.ReplacementContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.SwapContainerImpl
import kotlinx.serialization.json.Json
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

internal open class StonecutterBuildProperties @Inject constructor(objects: ObjectFactory) : DeprecatedBuildParams,
    VersionOperations<ParsedVersion> by LenientOperations {
    val data = StonecutterBuildData()

    override val constants: ConstantContainerImpl = ConstantContainerImpl(data.constants as MutableMap)
    override val dependencies: DependencyContainerImpl = DependencyContainerImpl(data.dependencies as MutableMap)
    override val swaps: SwapContainer = SwapContainerImpl(data.swaps as MutableMap)
    override val replacements: ReplacementContainer = ReplacementContainerImpl(data.replacements, objects)
    override val filters: FilterContainer = FilterContainerImpl(data.extensions as MutableSet, data.excludes as MutableSet)

    internal fun encode() = Json.encodeToString(TransformParameters(data.swaps, data.constants, data.dependencies, data.replacements))
}