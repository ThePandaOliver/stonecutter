import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.file.shouldContainFile
import io.kotest.matchers.file.shouldNotContainFile
import io.kotest.matchers.string.shouldContain
import util.*

class BuildscriptResolutionTest : FunSpec({
    val directory = tempdir()
    isolationMode = IsolationMode.InstancePerLeaf
    tags(HeavyTest)

    context("controller") {
        test("default") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter.create(rootProject) { 
                    versions("example")
                }
            """.trimIndent()

            build(directory) {
                directory shouldContainFile "stonecutter.gradle.kts"
            }
        }

        test("existing") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter.create(rootProject) { 
                    versions("example")
                }
            """.trimIndent()
            directory.resolve("stonecutter.gradle") with """
                $SETTINGS_TEMPLATE
                stonecutter.active("example")
            """.trimIndent()

            build(directory) {
                directory shouldNotContainFile "stonecutter.gradle.kts"
            }
        }

        test("top-level override") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter {
                    kotlinController = false
                    create(rootProject) { 
                        versions("example")
                    }
                }
            """.trimIndent()

            build(directory) {
                directory shouldContainFile "stonecutter.gradle"
            }
        }

        test("tree override") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter {
                    create(rootProject) { 
                        kotlinController = false
                        versions("example")
                    }
                }
            """.trimIndent()

            build(directory) {
                directory shouldContainFile "stonecutter.gradle"
            }
        }
    }

    context("build") {
        val helloTask = """
            tasks.register("sayHello") {
                doFirst { println("Hello!") }
            }
        """.trimIndent()

        test("default") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter.create(rootProject) {
                    versions("example")
                }
            """.trimIndent()
            directory.resolve(BUILD) with helloTask

            build(directory, "sayHello") {
                output shouldContain "Hello!"
            }
        }

        test("existing") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter.create(rootProject) {
                    versions("example")
                }
            """.trimIndent()
            directory.resolve("build.gradle") with helloTask

            build(directory, "sayHello") {
                output shouldContain "Hello!"
            }
        }

        test("top-level override") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter {
                    centralScript = "example.gradle"
                    create(rootProject) {
                        versions("example")
                    }
                }
            """.trimIndent()
            directory.resolve("example.gradle") with helloTask

            build(directory, "sayHello") {
                output shouldContain "Hello!"
            }
        }

        test("tree override") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter {
                    centralScript = "example.gradle"
                    create(rootProject) {
                        versions("example")
                    }
                }
            """.trimIndent()
            directory.resolve("example.gradle") with helloTask

            build(directory, "sayHello") {
                output shouldContain "Hello!"
            }
        }

        test("mapping override") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter {
                    create(rootProject) {
                        mapBuilds { _, _ -> "example.gradle" }
                        versions("example")
                    }
                }
            """.trimIndent()
            directory.resolve("example.gradle") with helloTask

            build(directory, "sayHello") {
                output shouldContain "Hello!"
            }
        }

        test("entry override") {
            directory.resolve(SETTINGS) with """
                $SETTINGS_TEMPLATE
                stonecutter {
                    centralScript = "unused.gradle"
                    create(rootProject) {
                        versions("example").buildscript("example.gradle")
                    }
                }
            """.trimIndent()
            directory.resolve("example.gradle") with helloTask

            build(directory, "sayHello") {
                output shouldContain "Hello!"
            }
        }
    }
})