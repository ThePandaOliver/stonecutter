import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldNotBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import util.BUILD
import util.CONTROLLER
import util.SETTINGS
import util.SETTINGS_TEMPLATE
import util.build
import util.with

public class TaskHookTest : StringSpec({
    isolationMode = IsolationMode.InstancePerLeaf
    val directory = tempdir()

    "aggregation" {
        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
            stonecutter.create(rootProject) { versions("1.20.1", "1.21.1") }
        """.trimIndent()

        directory.resolve(CONTROLLER) with """
            $SETTINGS_TEMPLATE
            stonecutter active "1.20.1"
            
            tasks.register("sayHelloAndGoodbye") {
                doLast { println("Goodbye!") }
                dependsOn(stonecutter.tasks.named("sayHello"))
            }
        """.trimIndent()

        directory.resolve(BUILD) with """
            tasks.register("sayHello") {
                doFirst { println("Hello!") }
            }
        """.trimIndent()

        build(directory, "sayHelloAndGoodbye") {
            val lastHello = output.lastIndexOf("Hello!")
            lastHello shouldBeGreaterThanOrEqual 0
            lastHello shouldNotBeGreaterThan output.indexOf("Goodbye!")
        }
    }

    "filtered aggregation" {
        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
            stonecutter.create(rootProject) { versions("1.20.1", "1.21.1") }
        """.trimIndent()

        directory.resolve(CONTROLLER) with """
            $SETTINGS_TEMPLATE
            stonecutter active "1.20.1"
            
            tasks.register("sayHelloAndGoodbye") {
                doLast { println("Goodbye!") }
                dependsOn(stonecutter.tasks.named("sayHello") {
                    metadata.project != "1.21.1"
                })
            }
        """.trimIndent()

        directory.resolve(BUILD) with """
            tasks.register("sayHello") {
                doFirst { println("Hello!") }
            }
        """.trimIndent()

        build(directory, "sayHelloAndGoodbye") {
            task(":1.21.1:sayHello") shouldBe null
            task(":1.20.1:sayHello") shouldNotBe null
        }
    }

    "switch" {
        directory.resolve(SETTINGS) with """
            $SETTINGS_TEMPLATE
            stonecutter.create(rootProject) { versions("1.20.1", "1.21.1") }
        """.trimIndent()

        directory.resolve(CONTROLLER) with $$"""
            $$SETTINGS_TEMPLATE
            stonecutter active "1.20.1"
            
            for ((_, task) in stonecutter.tasks.switch) task.configure {
                doLast { println("Just switched from ${stonecutter.current?.project}!") }
            }
        """.trimIndent()

        build(directory, "stonecutterSwitchTo1.21.1") {
            output shouldContain "Just switched from 1.20.1!"
            val controller = directory.resolve(CONTROLLER).readText()
            controller shouldContainOnlyOnce "stonecutter active" // Check for duplicates
            controller shouldContain "stonecutter active \"1.21.1\""
        }
    }
})