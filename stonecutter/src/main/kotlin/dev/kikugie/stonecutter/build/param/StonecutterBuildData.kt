package dev.kikugie.stonecutter.build.param

import dev.kikugie.stitcher.data.replacement.RegexReplacement
import dev.kikugie.stitcher.data.replacement.Replacement
import dev.kikugie.stitcher.data.replacement.ReplacementList
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stitcher.data.replacement.StringReplacement
import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.newInstance
import dev.kikugie.stonecutter.util.orEmpty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject
import kotlin.collections.MutableList
import kotlin.collections.first
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.toMutableSet

@Serializable(with = StonecutterBuildData.Serializer::class)
public abstract class StonecutterBuildData @Inject constructor(private val objects: ObjectFactory, private val factory: ProviderFactory, private val list: MutableList<Replacement>) {
    @get:Input @get:Optional public abstract val constantsProperty: MapProperty<Identifier, Boolean>
    @get:Input @get:Optional public abstract val dependenciesProperty: MapProperty<Identifier, Version>
    @get:Input @get:Optional public abstract val swapsProperty: MapProperty<Identifier, String>
    @get:Nested @get:Optional public abstract val replacementsProperty: ListProperty<ReplacementStub>

    init {
        constantsProperty.set(mutableMapOf())
        dependenciesProperty.set(mutableMapOf())
        swapsProperty.set(mutableMapOf())
        replacementsProperty.set(factory.provider { list.map(::toStub) })
    }

    private fun toStub(it: Replacement): ReplacementStub = when (it) {
        is StringReplacement -> toStringStub(it)
        is RegexReplacement -> toRegexStub(it)
    }

    private fun toStringStub(it: StringReplacement) = objects.newInstance<ReplacementStub> {
        type("STRING"); phase(it.phase.name); it.identifier?.let(identifier::set)
        sources(it.sources); target(it.target)
    }

    private fun toRegexStub(it: RegexReplacement) = objects.newInstance<ReplacementStub> {
        type("REGEX"); phase(it.phase.name); it.identifier?.let(identifier::set)
        sources(listOf(it.pattern.pattern)); target(it.target)
    }

    public interface ReplacementStub {
        @get:Input public val type: Property<String>
        @get:Input @get:Optional public val phase: Property<String>
        @get:Input @get:Optional public val identifier: Property<String>
        @get:Input public val sources: ListProperty<String>
        @get:Input public val target: Property<String>

        public fun toReplacement(): Replacement = when (val it = type()) {
            "STRING" -> toStringReplacement()
            "REGEX" -> toRegexReplacement()
            else -> error("Unknown type $it")
        }

        private fun toStringReplacement(): StringReplacement = StringReplacement(
            sources().toMutableSet(), target(), ReplacementPhase.valueOf(phase()), identifier.orNull
        )

        private fun toRegexReplacement(): RegexReplacement = RegexReplacement(
            sources().first().toRegex(), target(), ReplacementPhase.valueOf(phase()), identifier.orNull
        )
    }

    public object Serializer : KSerializer<StonecutterBuildData> {
        override val descriptor: SerialDescriptor get() = TransformParameters.serializer().descriptor

        override fun serialize(encoder: Encoder, value: StonecutterBuildData): Unit =
            encoder.encodeSerializableValue(TransformParameters.serializer(), convert(value))

        override fun deserialize(decoder: Decoder): StonecutterBuildData =
            throw UnsupportedOperationException()

        internal fun convert(value: StonecutterBuildData): TransformParameters =
            convert(value.constantsProperty, value.swapsProperty, value.dependenciesProperty, value.replacementsProperty)

        internal fun convert(
            constants: MapProperty<String, Boolean>,
            swaps: MapProperty<String, String>,
            dependencies: MapProperty<String, String>,
            replacements: ListProperty<ReplacementStub>
        ) = TransformParameters(
            swaps.orEmpty(),
            constants.orEmpty(),
            dependencies.orEmpty().mapValues { (_, v) -> LenientOperations.parse(v) },
            replacements.orEmpty().map { it.toReplacement() }.let { ReplacementList(it.toMutableList()) }
        )
    }
}