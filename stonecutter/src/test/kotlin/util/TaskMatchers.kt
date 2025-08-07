package util

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

internal infix fun BuildTask?.shouldReturn(expected: TaskOutcome) = shouldNotBeNull {
    outcome shouldBe expected
}

internal infix fun BuildTask?.shouldNotReturn(expected: TaskOutcome) = shouldNotBeNull {
    outcome shouldNotBe expected
}