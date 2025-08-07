@file:OptIn(ExperimentalSerializationApi::class)

import dev.kikugie.stonecutter.data.tree.json.SerializedTree
import dev.kikugie.stonecutter.data.tree.json.SerializedVersion
import dev.kikugie.stonecutter.data.tree.json.TreeScheme
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

private val JSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
    allowComments = true
    allowTrailingComma = true
}

private fun deserializeTree(@Language("JSON5") input: String) = JSON.decodeFromString(SerializedTree.serializer(), input)

class TreeSerializationTest : StringSpec({
    "by version plain" {
        val tree = deserializeTree("""
            {
              versions: ["1.20.1-fabric:1.20.1", "1.21.1"]
            }
        """.trimIndent())

        val versions = tree.run {
            val first = (schemes shouldHaveSize 1).first()
            first::class shouldBe TreeScheme.Plain::class
            (first as TreeScheme.Plain).versions
        }

        versions shouldHaveSize 2
        with(versions[0]) {
            project shouldNotBeEqual version
        }
        with(versions[1]) {
            project shouldBeEqual version
        }
    }

    "by version expanded" {
        val tree = deserializeTree("""
            {
              versions: [
                {
                  project: "1.20.1-fabric",
                  version: "1.20.1"
                },
                "1.21.1"
              ]
            }
        """.trimIndent())

        val versions = tree.run {
            val first = (schemes shouldHaveSize 1).first()
            first::class shouldBe TreeScheme.Plain::class
            (first as TreeScheme.Plain).versions
        }

        versions shouldHaveSize 2
        with(versions[0]) {
            project shouldNotBeEqual version
        }
        with(versions[1]) {
            project shouldBeEqual version
        }
    }

    "by branch default" {
        val tree = deserializeTree("""
            {
              branches: {
                "": ["1.20.1", "1.21.1"],
                "fabric": ["1.20.1", "1.21.1"],
              }
            }
        """.trimIndent())

        @Suppress("UNCHECKED_CAST")
        val schemes = tree.schemes as List<TreeScheme.Branched>
        schemes shouldHaveSize 1
        val first = schemes.first()

        first.branches[""]!! shouldContainAll first.branches["fabric"]!!
    }

    "by version inverted" {
        val tree = deserializeTree("""
            {
              versions: {
                "1.20.1": ["", "fabric"],
                "1.21.1": ["", "fabric"],
              }
            }
        """.trimIndent())

        @Suppress("UNCHECKED_CAST")
        val schemes = tree.schemes as List<TreeScheme.Inverted>
        schemes shouldHaveSize 1
        val first = schemes.first()

        first.versions[SerializedVersion("1.20.1")]!! shouldContainAll first.versions[SerializedVersion("1.21.1")]!!
    }
})