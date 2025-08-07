import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.intellij.lang.annotations.Language
import util.BUILD
import util.PROPERTIES
import util.SETTINGS
import util.SETTINGS_TEMPLATE
import util.build
import util.with

public class FlagTest : StringSpec({
    isolationMode = IsolationMode.InstancePerLeaf
    val directory = tempdir()

    "apply plugin to nodes" {
        withClue("Default:") {
            StonecutterFlag.APPLY_PLUGIN_TO_NODES.default shouldBe true
        }

        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
    
            stonecutter.create(rootProject) { version("example") }
        """.trimIndent()

        @Language("properties") val properties = """
            dev.kikugie.stonecutter.auto_apply_plugin = false
        """.trimIndent()
        directory.resolve(PROPERTIES) with properties

        directory.resolve(BUILD) with """
            val active = stonecutter.current.isActive
        """.trimIndent()

        val exception = shouldThrow<UnexpectedBuildFailure> {
            build(directory)
        }

        exception.message shouldContain "Unresolved reference: stonecutter"
    }

    // FIXME: Trying to imitate IDEA sync causes 'Could not find included build with root directory'
    "generate sources on sync".config(enabled = false) {
        withClue("Default:") {
            StonecutterFlag.GENERATE_SOURCES_ON_SYNC.default shouldBe true
        }

        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
    
            stonecutter.create(rootProject) { version("1.20.1") }
        """.trimIndent()

        directory.resolve(BUILD) with """
            plugins { java }
        """.trimIndent()

        @Language("properties") val properties = """
            dev.kikugie.stonecutter.auto_apply_plugin = false
        """.trimIndent()
        directory.resolve(PROPERTIES) with properties

        build(directory, "-Didea.sync.active=true") {
            println(output)
        }
    }
})