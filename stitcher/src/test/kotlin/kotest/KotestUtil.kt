package kotest

import io.kotest.assertions.assertionCounter
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun <T> Iterator<T>.toList(): List<T> = buildList {
    while (hasNext()) add(next())
}

@JvmName("shouldBeOfNonnullType")
inline fun <reified T : Any> Any?.shouldBeOfType(crossinline action: T.() -> Unit = {}) =
    shouldNotBeNull { shouldBeOfType<T> { action() } }

inline infix fun <reified T : Any> Any.shouldBeOfType(action: T.() -> Unit = {}) {
    shouldBeInstanceOf<T>()
    action(this)
}

inline fun shouldThrow(cls: KClass<out Throwable>, action: () -> Unit) {
    assertionCounter.inc()
    val throwable = runCatching(action).exceptionOrNull()
    throwable.should { throwableMatcher(cls) }
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T : Any> Any.shouldBeInstanceOf(): T {
    contract {
        returns() implies (this@shouldBeInstanceOf is T)
    }
    this should instanceMatcher(T::class)
    return this as T
}

fun <T : Any> instanceMatcher(cls: KClass<T>): Matcher<Any> = object : Matcher<Any> {
    override fun test(value: Any): MatcherResult {
        val matches = value::class.isSubclassOf(cls)

        return object : MatcherResult {
            override fun failureMessage(): String = "Expected ${cls.qualifiedName} to be ${value::class.qualifiedName}"
            override fun negatedFailureMessage(): String = "Expected ${cls.qualifiedName} not to be ${value::class.qualifiedName}"
            override fun passed(): Boolean = matches
        }
    }
}

fun <T : Throwable> throwableMatcher(cls: KClass<T>): Matcher<Throwable> = object : Matcher<Throwable?> {
    override fun test(value: Throwable?): MatcherResult =
        if (value == null) testNoThrowable() else testThrowable(value)

    private fun testNoThrowable() = object : MatcherResult {
        override fun failureMessage(): String = "Expected ${cls.qualifiedName} to be thrown, but nothing was thrown"
        override fun negatedFailureMessage(): String = "Expected nothing to be thrown"
        override fun passed(): Boolean = false
    }

    private fun testThrowable(throwable: Throwable): MatcherResult {
        return object : MatcherResult {
            override fun failureMessage(): String = "Expected ${cls.qualifiedName} to be thrown, but ${throwable::class.qualifiedName} was thrown"
            override fun negatedFailureMessage(): String = "Expected ${cls.qualifiedName} not to be thrown, but ${throwable::class.qualifiedName} was thrown"
            override fun passed(): Boolean = throwable::class.isSubclassOf(cls)
        }
    }
}