package dev.kikugie.stonecutter.data.service

import dev.kikugie.stitcher.transformer.TransformParameters
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

internal abstract class ParameterCacheService : BuildService<ParameterCacheService.Properties> {
    interface Properties : BuildServiceParameters {
        val cache: MapProperty<String, TransformParameters>
    }

    operator fun set(key: String, value: TransformParameters) = parameters.cache.put(key, value)
    operator fun set(key: String, value: Provider<TransformParameters>) = parameters.cache.put(key, value)
    operator fun get(key: String): Provider<TransformParameters> = parameters.cache.getting(key)
}