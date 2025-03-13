package v2

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import kotlin.coroutines.RestrictsSuspension
import kotlin.reflect.KClass

val YAML = Yaml(
    configuration = Yaml.default.configuration.copy(
        strictMode = false
    )
)

inline fun <reified T> T.yaml() = YAML.encodeToString(this)

interface TestInstance {
    val name: String
    val display: String
    fun run()
}

@RestrictsSuspension
class TestListBuilder<T : TestInstance>(cls: KClass<T>) : Iterable<DynamicTest> {
    private val constructor = findNamedConstructor(cls)
    private val instances = mutableListOf<TestConfiguration>()

    operator fun String.invoke(block: T.() -> Unit) {
        instances += TestConfiguration(this, block)
    }

    private fun findNamedConstructor(cls: KClass<T>): (String) -> T {
        val match = cls.constructors.find { it.parameters.size == 1 && it.parameters.first().type.classifier == String::class }
        requireNotNull(match) { "Test class ${cls.qualifiedName} must have a constructor with a single String argument" }
        return match::call
    }

    override fun iterator(): Iterator<DynamicTest> = TestIterator()

    private inner class TestConfiguration(val name: String, val build: T.() -> Unit) {
        fun build(): T = constructor(name).apply(build)

        fun execute() = build().run {
            println(display); run()
        }
    }

    private inner class TestIterator : Iterator<DynamicTest> {
        private var cursor = 0
        override fun hasNext(): Boolean = cursor < instances.size
        override fun next(): DynamicTest = instances[cursor++]
            .let { dynamicTest(it.name) { it.execute() } }
    }
}

inline fun <reified T : TestInstance> test(builder: TestListBuilder<T>.() -> Unit) =
    TestListBuilder(T::class).apply(builder)