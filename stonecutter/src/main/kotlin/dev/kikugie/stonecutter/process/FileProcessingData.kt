package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

internal interface FileProcessingData {
    val constants: MapProperty<Identifier, Boolean>
    val swaps: MapProperty<Identifier, String>
    val dependencies: MapProperty<Identifier, AnyVersion>
    val replacements: ListProperty<ReplacementData>

    interface ReplacementData {
        val type: Property<String>
        val phase: Property<String>
        val id: Property<String>
        val sources: ListProperty<String>
        val target: Property<String>
    }
}
