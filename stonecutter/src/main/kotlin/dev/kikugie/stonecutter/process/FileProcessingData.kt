package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

public interface FileProcessingData {
    public val constants: MapProperty<Identifier, Boolean>
    public val swaps: MapProperty<Identifier, String>
    public val dependencies: MapProperty<Identifier, AnyVersion>
    public val replacements: ListProperty<ReplacementData>

    public interface ReplacementData {
        public val type: Property<String>
        public val phase: Property<String>
        public val id: Property<String>
        public val sources: ListProperty<String>
        public val target: Property<String>
    }
}
