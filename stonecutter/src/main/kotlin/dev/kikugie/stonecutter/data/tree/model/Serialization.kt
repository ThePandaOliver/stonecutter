package dev.kikugie.stonecutter.data.tree.model

import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.controller.flag.FlagContainerImpl
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

public object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.nio.file.Path", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Path): Unit = encoder.encodeString(value.invariantSeparatorsPathString)
    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}

@OptIn(ExperimentalSerializationApi::class)
public object FlagContainerJsonSerializer : KSerializer<FlagContainer> {
    @Serializable @JvmInline
    private value class JsonFlagContainer(val map: Map<String, JsonPrimitive>)
    override val descriptor: SerialDescriptor = JsonFlagContainer.serializer().descriptor

    override fun serialize(encoder: Encoder, value: FlagContainer): Unit = (value as FlagContainerImpl).flags
        .mapValues { (_, value) -> value.asJsonPrimitive() }
        .let { encoder.encodeSerializableValue(JsonFlagContainer.serializer(), JsonFlagContainer(it)) }

    override fun deserialize(decoder: Decoder): FlagContainer = JsonFlagContainer.serializer().deserialize(decoder).map
        .mapValues { (key, value) -> value.asAny(key) }
        .let(::FlagContainerImpl)

    private fun Any.asJsonPrimitive(): JsonPrimitive = when (this) {
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> error("Unsupported type: ${this::class.qualifiedName}")
    }

    private fun JsonPrimitive.asAny(key: String): Any = when(StonecutterFlag.named(key).default) {
        is Boolean -> boolean
        is Int -> int
        is Long -> long
        is Float -> float
        is Double -> double
        is String -> content
        else -> throw SerializationException("Unsupported type for flag $key")
    }
}