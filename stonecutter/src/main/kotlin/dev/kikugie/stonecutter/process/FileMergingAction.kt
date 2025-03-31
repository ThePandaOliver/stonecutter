package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.process.FileMergingAction.Parameters
import dev.kikugie.stonecutter.invoke
import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

internal abstract class FileMergingAction : WorkAction<Parameters> {
    interface Parameters : WorkParameters {
        val cache: RegularFileProperty
        val source: RegularFileProperty
        val override: RegularFileProperty
        val output: RegularFileProperty
    }

    private val bestSourceMatch: RegularFileProperty
        get() = parameters.override.takeIfExists()
            ?: parameters.cache.takeIfExists()
            ?: parameters.source

    override fun execute() {
        val match = bestSourceMatch.asFile()
        val output = parameters.output.get().asFile
        if (match != output) {
            output.parentFile.mkdirs()
            match.copyTo(output, overwrite = true)
        }
    }

    private fun RegularFileProperty.takeIfExists() =
        if (isPresent && asFile().exists()) this else null
}