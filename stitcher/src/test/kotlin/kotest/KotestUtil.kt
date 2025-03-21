package kotest

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

inline fun <reified T : Any> Any?.shouldBeOfType(crossinline action: T.() -> Unit) =
    shouldNotBeNull { shouldBe<T> { action() } }

inline infix fun <reified T : Any> Any.shouldBe(action: T.() -> Unit = {}) {
    shouldBeInstanceOf<T>()
    action(this)
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
            override fun failureMessage(): String = "Expected ${cls.qualifiedName}, actual ${value::class.qualifiedName}"
            override fun negatedFailureMessage(): String = "Expected ${cls.qualifiedName} not to be ${value::class.qualifiedName}"
            override fun passed(): Boolean = matches
        }
    }
}