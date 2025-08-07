import dev.kikugie.stonecutter.data.StonecutterProject
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.intellij.lang.annotations.Language
import util.*

public class TreeBuildingTest : StringSpec({
    val directory = tempdir()

    "basic configuration" {
        val task = "printStonecutterMetadata"

        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
            
            stonecutter.create(rootProject) {
                versions("1.20.1", "1.21.1")
                version("snapshot", "1.21.9-alpha.25.39")
            }
        """.trimIndent()

        directory.resolve(BUILD) with """
            tasks.register("$task") {
                doFirst {
                    println(stonecutter.current)
                }
            }
        """.trimIndent()

        build(directory, task) {
            task(":1.20.1:$task") shouldReturn SUCCESS
            task(":1.21.1:$task") shouldReturn SUCCESS
            task(":snapshot:$task") shouldReturn SUCCESS

            output shouldContainOnlyOnce StonecutterProject("1.20.1", "1.20.1").toString()
            output shouldContainOnlyOnce StonecutterProject("1.21.1", "1.21.1").toString()
            output shouldContainOnlyOnce StonecutterProject("snapshot", "1.21.9-alpha.25.39").toString()
        }
    }

    "inconsistent version" {
        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
            
            stonecutter.create(rootProject) {
                version("snapshot", "1.21.9-alpha.25.39")
                branch("example") {
                    version("snapshot", "1.21.9-alpha.25.38")
                }
            }
        """.trimIndent()

        val exception = shouldThrow<UnexpectedBuildFailure> {
            build(directory)
        }

        exception.message shouldContain "Project 'snapshot' is registered with a different version"
    }

    // FIXME: Overriding projects in the same branch should probably replace the version
    "override json test".config(enabled = false) {
        val task = "printStonecutterMetadata"
        @Language("JSON5") val json = """
            {
                versions: ["1.20.1", "snapshot"]
            }
        """.trimIndent()

        directory.resolve("versions.json5") with json

        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
            stonecutter.create(rootProject, file("versions.json5")) {
                version("snapshot", "1.21.9")
            }
        """.trimIndent()

        directory.resolve(BUILD) with """
            tasks.register("$task") {
                doFirst {
                    println(stonecutter.current)
                }
            }
        """.trimIndent()

        build(directory, task) {
            output shouldContainOnlyOnce StonecutterProject("snapshot", "1.21.9").toString()
        }
    }
})