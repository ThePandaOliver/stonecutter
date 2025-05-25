package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.data.service.ParameterCacheService
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.registerIfAbsent
import java.util.concurrent.ConcurrentHashMap

@JvmInline
internal value class StonecutterSetup(val settings: Settings) {
    fun apply() {
        registerBuildServices()
    }

    private fun registerBuildServices() = settings.gradle.sharedServices.apply {
        registerIfAbsent("SCParameterCache", ParameterCacheService::class) {
            parameters.cache.set(ConcurrentHashMap())
        }
    }
}