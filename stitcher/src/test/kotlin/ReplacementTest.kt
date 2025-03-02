import dev.kikugie.stitcher.data.Replacement
import dev.kikugie.stitcher.data.Replacement.Companion.string
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

object ReplacementTest {
    val RESOLUTION_SAMPLES = buildList {
        add("simple separate") {
            entry("a", "b")
            entry("c", "d")

            expect(setOf("a"), "b")
            expect(setOf("c"), "d")
        }

        add("simple merged") {
            entry("a", "c")
            entry("b", "c")

            expect(setOf("a", "b"), "c")
        }

        add("simple loop") {
            entry("a", "b")
            entry("b", "a")

            exception<IllegalArgumentException>()
        }

        add("simple split") {
            entry("a", "b")
            entry("b", "d")
            entry("a", "c")

            exception<IllegalArgumentException>()
        }
    }

    @TestFactory
    fun `test string resolution`() = RESOLUTION_SAMPLES.map {
        DynamicTest.dynamicTest(it.name) { check(it) }
    }

    fun check(sample: ResolutionSample) {
        val list = mutableListOf<Replacement>()
        fun run() = sample.entries.forEach { list.string(it.from, it.to, it.phase, it.identifier) }.also {
            val str = buildString {
                appendLine("Parsed replacements for '${sample.name}':")
                list.forEach { appendLine("  $it") }
            }
            println(str)
        }

        when {
            sample.exception != null -> Assertions.assertThrows(sample.exception!!.java) { run() }
            sample.expected.isNotEmpty() -> run().also {
                val expected = sample.expected.joinToString("\n")
                val actual = list.joinToString("\n")
                Assertions.assertEquals(expected, actual) { "Incorrect replacements" }
            }
            else -> run()
        }
    }

    inline fun MutableList<ResolutionSample>.add(name: String, build: ResolutionSample.() -> Unit) =
        add(ResolutionSample(name).apply(build))

    class ResolutionSample(val name: String) {
        var exception: KClass<out Throwable>? = null
        val entries = mutableListOf<Entry>()
        val expected = mutableListOf<Replacement.StringReplacement>()

        inline fun <reified T : Throwable> exception() { exception = T::class }

        fun expect(sources: Set<String>, target: String, phase: Replacement.Phase = Replacement.Phase.FIRST, identifier: String? = null) =
            expected.add(Replacement.StringReplacement(sources.toMutableSet(), target, phase, identifier))

        fun entry(from: String, to: String, phase: Replacement.Phase = Replacement.Phase.FIRST, identifier: String? = null) =
            entries.add(Entry(from, to, phase, identifier))

        class Entry(val from: String, val to: String, val phase: Replacement.Phase, val identifier: String? = null)
    }
}