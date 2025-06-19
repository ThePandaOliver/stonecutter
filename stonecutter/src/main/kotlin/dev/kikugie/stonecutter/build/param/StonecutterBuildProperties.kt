package dev.kikugie.stonecutter.build.param

import dev.kikugie.stitcher.data.replacement.Replacement
import dev.kikugie.stonecutter.data.dsl.*
import dev.kikugie.stonecutter.data.dsl.impl.*
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject
import dev.kikugie.semver.data.Version as ParsedVersion

public abstract class StonecutterBuildProperties @Inject constructor(
    override val node: ProjectNode,
    objects: ObjectFactory,
    factory: ProviderFactory
) : DeprecatedBuildConfig, Named, VersionOperations<ParsedVersion> by LenientOperations {
    private val list: MutableList<Replacement> = mutableListOf()
    internal val data: StonecutterBuildData = objects.newInstance(factory, list)

    override val constants: ConstantContainer = ConstantContainerImpl(factory, data.constantsProperty)
    override val dependencies: DependencyContainer = DependencyContainerImpl(factory, data.dependenciesProperty)
    override val swaps: SwapContainer = SwapContainerImpl(factory, data.swapsProperty)
    override val replacements: ReplacementContainer = ReplacementContainerImpl(objects, list)
    override val filters: PatternFilterable = PatternSet()

    override fun getName(): String = "StonecutterBuild@${node.hierarchy}"
}

