package kotest

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import io.kotest.core.spec.style.scopes.RootScope
import io.kotest.core.test.TestScope
import io.kotest.datatest.withData

val YAML = Yaml(
    configuration = Yaml.default.configuration.copy(
        strictMode = false
    )
)
inline fun <reified T : DataTestInstance> RootScope.withData(name: String) {
    withData(datatest<T>(name)) { it.execute(this) }
}

inline fun <reified T : DataTestInstance> datatest(name: String): Map<String, T> = requireNotNull(T::class.java.classLoader.getResourceAsStream("datatest/$name")) {
    "No data test file found for $name"
}.use {
    YAML.decodeFromStream<Map<String, T>>(it)
}

interface DataTestInstance {
    fun display(): String
    fun execute(scope: TestScope)
}