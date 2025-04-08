@file:Suppress("UnstableApiUsage", "unused")

package dev.kikugie.stonecutter.util

import dev.kikugie.stonecutter.StonecutterPlugin
import org.gradle.api.plugins.PluginAware
import org.gradle.api.problems.Problem
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
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
    val PROBLEM_GROUP = ProblemGroup.create("stonecutter-problems", "Stonecutter problems")
    val GROOVY_BUILD_USED = ProblemId.create("groovy-build-used", "Groovy compatibility", PROBLEM_GROUP)
}

internal class SCProblemSpec {
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

internal inline fun ProblemReporter.new(id: ProblemId, config: SCProblemSpec.() -> Unit): Problem =
    create(id, SCProblemSpec().apply(config)::build)

internal inline fun ProblemReporter.print(id: ProblemId, config: SCProblemSpec.() -> Unit): Unit =
    report(id, SCProblemSpec().apply(config)::build)

internal inline fun ProblemReporter.error(id: ProblemId, err: Throwable, config: SCProblemSpec.() -> Unit): Nothing {
    throw throwing(err, id, SCProblemSpec().apply(config)::build)
}