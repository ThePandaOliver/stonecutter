package util

import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

internal infix fun <T> List<T>.shouldHaveAt(index: Int): T {
    shouldHaveAtLeastSize(index + 1)
    return get(index)
}

internal infix fun <K: Any, V: Any> Map<K, V>.shouldHaveAt(key: K): V {
    shouldHaveKey(key)
    return get(key)!!
}
internal infix fun BuildTask?.shouldReturn(expected: TaskOutcome) = shouldNotBeNull {
    outcome shouldBe expected
}

internal infix fun BuildTask?.shouldNotReturn(expected: TaskOutcome) = shouldNotBeNull {
    outcome shouldNotBe expected
}