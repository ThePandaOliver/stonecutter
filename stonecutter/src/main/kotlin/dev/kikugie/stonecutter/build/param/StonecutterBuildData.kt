package dev.kikugie.stonecutter.build.param

import dev.kikugie.stitcher.data.replacement.*
import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.data.dsl.impl.ConstantContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.DependencyContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.FilterContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.ReplacementContainerImpl
import dev.kikugie.stonecutter.data.dsl.impl.SwapContainerImpl
import dev.kikugie.stonecutter.process.FileProcessingData
import dev.kikugie.stonecutter.process.FileProcessingData.ReplacementData
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.newInstance
import org.gradle.api.model.ObjectFactory
import java.nio.file.Path
import javax.inject.Inject

internal open class StonecutterBuildData @Inject constructor(private val dir: Path, private val objects: ObjectFactory) : StonecutterBuildParams {
    internal companion object {
        val DEFAULT_EXTENSIONS = setOf("java", "kt", "kts", "groovy", "gradle", "scala", "sc", "json5", "hjson")
    }

    override val constants: ConstantContainerImpl = ConstantContainerImpl()
    override val dependencies: DependencyContainerImpl = DependencyContainerImpl()
    override val swaps: SwapContainerImpl = SwapContainerImpl()
    override val replacements: ReplacementContainerImpl = ReplacementContainerImpl(objects)
    override val filters: FilterContainerImpl = FilterContainerImpl()

    internal fun asProcessingData(key: Identifier, version: Version): FileProcessingData = objects.newInstance {
        constants.set(this@StonecutterBuildData.constants.toMap())
        swaps.set(this@StonecutterBuildData.swaps.toMap())
        dependencies.set(this@StonecutterBuildData.dependencies.withDefaultVersion(key, version))
        replacements.set(this@StonecutterBuildData.replacements.replacements.map { it.asReplacementData() })
    }

    private fun Replacement.asReplacementData(): ReplacementData = when (this) {
        is StringReplacement -> objects.newInstance {
            type("STRING"); phase(this@asReplacementData.phase.name); this@asReplacementData.identifier?.let(id::set)
            sources(this@asReplacementData.sources); target(this@asReplacementData.target)
        }
        is RegexReplacement -> objects.newInstance {
            type("STRING"); phase(this@asReplacementData.phase.name); this@asReplacementData.identifier?.let(id::set)
            sources(listOf(this@asReplacementData.pattern.pattern)); target(this@asReplacementData.target)
        }
    }

    private fun Map<Identifier, Version>.withDefaultVersion(key: Identifier, version: Version) = toMutableMap().apply {
        getOrDefault(key, version).let { this[key] = it; this[""] = it }
    }
}