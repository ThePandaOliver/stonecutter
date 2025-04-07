@file:Suppress("UnstableApiUsage", "unused")

package dev.kikugie.stonecutter.util

import dev.kikugie.stonecutter.StonecutterPlugin
import org.gradle.api.plugins.PluginAware
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Severity
import org.gradle.kotlin.dsl.getPlugin
import java.io.File

@Suppress("NOTHING_TO_INLINE")
internal inline fun report(message: String): Nothing {
    throw IllegalArgumentException(message)
}

internal object SCProblems {
    val PLUGIN_VERSION_MISMATCH = SCProblemID("plugin-version-mismatch", "Plugin version mismatch")
    val GROOVY_BUILD_USED = SCProblemID("groovy-build-used", "Groovy compatibility")
}

internal object SCProblemGroup : ProblemGroup {
    override fun getName(): String = "stonecutter-problems"
    override fun getDisplayName(): String = "Stonecutter problems"
    override fun getParent(): ProblemGroup? = null
}

internal class SCProblemID(val id: String, val name: String, val parent: ProblemGroup = SCProblemGroup)

internal class SCProblemSpec(val id: SCProblemID) {
    var label: String? = null
    var details: String? = null
    var solution: String? = null
    var documentation: String? = null

    var file: File? = null
    var exception: Throwable? = null
    var severity: Severity = Severity.ERROR
    private var addStackTraces: Boolean = false

    fun trace() { addStackTraces = true }

    fun build(spec: ProblemSpec) {
        spec.id(id.id, id.name, id.parent)
        details?.let(spec::details)
        solution?.let(spec::solution)
        documentation?.let(spec::documentedAt)
        exception?.let(spec::withException)
        severity.let(spec::severity)
        file?.let { spec.fileLocation(it.absolutePath) }
    }
}

internal val PluginAware.problemReporter: ProblemReporter
    get() = plugins.getPlugin(StonecutterPlugin::class).reporter

internal inline fun ProblemReporter.reporting(id: SCProblemID, config: SCProblemSpec.() -> Unit) =
    reporting(SCProblemSpec(id).apply(config)::build)

internal inline fun ProblemReporter.throwing(id: SCProblemID, config: SCProblemSpec.() -> Unit) =
    throwing(SCProblemSpec(id).apply(config)::build)