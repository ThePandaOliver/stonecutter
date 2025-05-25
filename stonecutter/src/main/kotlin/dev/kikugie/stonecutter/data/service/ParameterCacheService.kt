package dev.kikugie.stonecutter.data.service

import dev.kikugie.stitcher.transformer.TransformParameters
import dev.kikugie.stonecutter.util.invoke
import kotlinx.serialization.json.Json
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap

internal abstract class ParameterCacheService : BuildService<ParameterCacheService.Properties> {
    interface Properties : BuildServiceParameters {
        val cache: Property<ConcurrentHashMap<String, TransformParameters>>
    }

    operator fun get(json: String): TransformParameters = parameters.cache().computeIfAbsent(json) { Json.decodeFromString(it) }
}