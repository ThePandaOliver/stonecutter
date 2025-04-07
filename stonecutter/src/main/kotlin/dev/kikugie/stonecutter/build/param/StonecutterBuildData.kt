package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.VersionParser
import dev.kikugie.stitcher.data.replacement.*
import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.build.param.ReplacementVariants.RegexReplacementBuilder
import dev.kikugie.stonecutter.build.param.ReplacementVariants.StringReplacementBuilder
import dev.kikugie.stonecutter.data.build.FileProcessingFilter
import dev.kikugie.stonecutter.data.build.ParameterMap
import dev.kikugie.stonecutter.data.build.newParameterMap
import dev.kikugie.stonecutter.util.newInstance
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.process.FileProcessingData
import dev.kikugie.stonecutter.process.FileProcessingData.ReplacementData
import dev.kikugie.stonecutter.util.isIdentifier
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import java.nio.file.Path
import javax.inject.Inject

internal open class StonecutterBuildData @Inject constructor(private val dir: Path, private val objects: ObjectFactory) : StonecutterBuildParams {
    internal companion object {
        val DEFAULT_EXTENSIONS = setOf("java", "kt", "kts", "groovy", "gradle", "scala", "sc", "json5", "hjson")
    }

    override val swaps: ParameterMap<Identifier, String> = newParameterMap(
        keyCheck = { checkKey(it, "Swap") }
    )

    override val consts: ParameterMap<Identifier, Boolean> = newParameterMap(
        keyCheck = { checkKey(it, "Constant") }
    )

    override val dependencies: ParameterMap<Identifier, String> = newParameterMap(
        keyCheck = { checkKey(it, "Dependency") },
        valueCheck = {
            require(it.isNotBlank()) { "Dependency value '$it' must not be blank." }
            require(isIdentifier(it) || it.runCatching { VersionParser.parseLenient(it, full = true) }.isSuccess)
            { "Dependency value '$it' must be a valid identifier or semver." }
        }
    )
    override val filter: FileProcessingFilter
        get() = FileProcessingFilter(allowedExtensions, excludedFiles)

    private val replacements: ReplacementList = ReplacementList()
    private val allowedExtensions: MutableSet<String> = DEFAULT_EXTENSIONS.toMutableSet()
    private val excludedFiles: MutableSet<Path> = mutableSetOf()

    override fun replacement(direction: Boolean, from: String, to: String, phase: String, id: Identifier?) {
        require(id == null || isIdentifier(id)) { "Invalid identifier: '$id'" }
        val realPhase = ReplacementPhase.valueOf(phase.uppercase())
        if (direction) replacements.addString(from, to, realPhase, id)
        else replacements.addString(to, from, realPhase, id)
    }

    override fun replacement(
        direction: Boolean,
        fromPattern: String,
        toValue: String,
        reversePattern: String,
        reverseValue: String,
        phase: String,
        id: Identifier?,
    ) {
        require(id == null || isIdentifier(id)) { "Invalid identifier: '$id'" }
        val realPhase = ReplacementPhase.valueOf(phase.uppercase())
        if (direction) replacements.addRegex(fromPattern.toRegex(), toValue, realPhase, id)
        else replacements.addRegex(reversePattern.toRegex(), reverseValue, realPhase, id)
    }

    override fun stringReplacement(build: StringReplacementBuilder.() -> Unit): Unit =
        objects.newInstance<StringReplacementBuilder>().build(this)

    override fun regexReplacement(build: RegexReplacementBuilder.() -> Unit): Unit =
        objects.newInstance<RegexReplacementBuilder>().build(this)

    override fun allowExtensions(extensions: Iterable<String>) {
        allowedExtensions += extensions
    }

    override fun overrideExtensions(extensions: Iterable<String>) {
        allowedExtensions.clear(); allowedExtensions += extensions
    }

    override fun excludeFiles(files: Iterable<String>): Unit = files.forEach {
        require(it.startsWith("src/")) { "Filtered files must be in the src/ directory" }
        excludedFiles.add(dir.resolve(it))
    }

    internal fun asProcessingData(key: Identifier, version: AnyVersion): FileProcessingData = objects.newInstance {
        constants.set(this@StonecutterBuildData.consts)
        swaps.set(this@StonecutterBuildData.swaps)
        dependencies.set(this@StonecutterBuildData.dependencies.withDefaultVersion(key, version))
        replacements.set(this@StonecutterBuildData.replacements.map { it.asReplacementData() })
    }

    private fun Replacement.asReplacementData(): ReplacementData = when (this) {
        is StringReplacement -> objects.newInstance {
            type("STRING"); phase(this@asReplacementData.phase.name); this@asReplacementData.identifier?.let(id::set)
            sources(this@asReplacementData.sources); target(this@asReplacementData.target)
        }
        is RegexReplacement -> objects.newInstance {
            type("STRING"); phase(this@asReplacementData.phase.name); this@asReplacementData.identifier?.let(id::set)
            sources(listOf(this@asReplacementData.pattern.pattern)); target(this@asReplacementData.target)
        }
    }

    private fun ParameterMap<Identifier, AnyVersion>.withDefaultVersion(key: Identifier, version: AnyVersion) = toMutableMap().apply {
        getOrDefault(key, version).let { this[key] = it; this[""] = it }
    }

    private fun checkKey(key: String, type: String) {
        require(key.isNotBlank()) { "$type key '$key' must not be blank." }
        require(isIdentifier(key)) { "$type key '$key' must be a valid identifier." }
    }
}