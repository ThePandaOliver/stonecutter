@file:OptIn(ExperimentalContracts::class)

package dev.kikugie.stonecutter.util

import dev.kikugie.stonecutter.ActiveReference
import dev.kikugie.stonecutter.ProjectReference
import org.gradle.api.file.RegularFile
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Provider
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.text.trimStart

private tailrec fun resolveActive(value: Any?): Any? = when(value) {
    null, is String, is File -> value
    is RegularFile -> resolveActive(value.asFile)
    is Provider<*> -> resolveActive(value.orNull)
    else -> error("Invalid active project type ${value::class.qualifiedName}")
}

@JvmInline
internal value class ActiveProvider private constructor(private val value: Any?) {
    companion object {
        fun of(value: ActiveReference) = ActiveProvider(resolveActive(value))
    }

    inline fun ifString(action: (String) -> Unit): ActiveProvider {
        contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
        if (value is String) action(value)
        return this
    }

    inline fun ifFile(action: (File) -> Unit): ActiveProvider {
        contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
        if (value is File) action(value)
        return this
    }
}

private tailrec fun resolveProject(value: Any, settings: Settings): ProjectDescriptor = when(value) {
    is CharSequence -> include(value.toString(), settings)
    is ProjectDescriptor -> include(value.path, settings)
    is Provider<*> -> resolveProject(value, settings)
    else -> error("Unsupported project type ${value::class.qualifiedName}")
}

private fun include(path: String, settings: Settings): ProjectDescriptor {
    val trimmed = path.trimStart(':')
    if (trimmed.isNotEmpty()) settings.include(trimmed)
    return settings.project(":$trimmed")
}

@JvmInline
internal value class ProjectProvider private constructor(val value: ProjectDescriptor) {
    companion object {
        fun of(value: ProjectReference, settings: Settings) = ProjectProvider(resolveProject(value, settings))
    }
}