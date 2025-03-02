package dev.kikugie.stonecutter.data.model

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.KSerializer
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ModelLoader {
    private val YAML = Yaml(
        configuration = Yaml.default.configuration.copy(
            strictMode = false,
            encodeDefaults = false
        )
    )

    internal fun <T> save(location: Path, model: T, serializer: KSerializer<T>): Result<Unit> = location.runCatching {
        val yaml = YAML.encodeToString(serializer, model)
        parent.createDirectories()
        writeText(
            yaml,
            Charsets.UTF_8,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    internal fun <T> load(location: Path, serializer: KSerializer<T>): Result<T> = location.runCatching {
        if (location.notExists()) throw NoSuchFileException(location.toFile())
        val text = readText(Charsets.UTF_8)
        YAML.decodeFromString(serializer, text)
    }
}