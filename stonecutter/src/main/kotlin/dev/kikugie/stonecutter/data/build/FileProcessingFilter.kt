package dev.kikugie.stonecutter.data.build

import java.nio.file.Path
import kotlin.io.path.extension

public data class FileProcessingFilter(val allowedExtensions: Set<String>, val excludedFiles: Set<Path>) {
    public fun filter(file: Path): Boolean = file.extension in allowedExtensions
        && excludedFiles.none { file.startsWith(it) }
}
