package dev.kikugie.stwotcher.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data object IntRangeSerializer : KSerializer<IntRange> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntRange", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IntRange) =
        encoder.encodeString("${value.first}..<${value.last + 1}")

    override fun deserialize(decoder: Decoder): IntRange {
        var (start, end) = decoder.decodeString().split("..", limit = 2)
        val isExclusive = end.startsWith('<')
        if (isExclusive) end = end.drop(1)

        val startInt = requireNotNull(start.toIntOrNull()) { "Invalid start of IntRange: $start" }
        var endInt = requireNotNull(end.toIntOrNull()) { "Invalid end of IntRange: $end" }
        if (!isExclusive) endInt++
        return startInt..<endInt
    }
}