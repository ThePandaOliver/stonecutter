@file:Suppress("unused") @file:OptIn(ExperimentalContracts::class)

package dev.kikugie.stonecutter.util

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private object LoggerCache {
    val loggers: MutableMap<String, Logger> = ConcurrentHashMap()
    val Class<*>.cleanName
        get() = name.substringBefore('$').let {
            if (!it.endsWith("Kt")) it
            else it.substringBeforeLast("Kt")
        }

    fun getLogger(obj: Any) = getLogger(obj.javaClass)
    fun getLogger(cls: Class<*>) = getLogger(cls.cleanName)
    fun getLogger(name: String) = loggers.computeIfAbsent(name, Logging::getLogger)
}

private class LoggerDelegate : ReadOnlyProperty<Any, Logger> {
    private var logger: Logger? = null
    override fun getValue(thisRef: Any, property: KProperty<*>): Logger =
        logger ?: LoggerCache.getLogger(thisRef).also { logger = it }
}

internal fun logger(): ReadOnlyProperty<Any, Logger> = LoggerDelegate()
internal fun logger(name: String): Lazy<Logger> = lazy { LoggerCache.getLogger(name) }

internal inline fun Logger.trace(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isTraceEnabled) trace(message())
}

internal inline fun Logger.trace(err: Throwable, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isTraceEnabled) trace(message(), err)
}

internal inline fun Logger.debug(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isDebugEnabled) debug(message())
}

internal inline fun Logger.debug(err: Throwable, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isDebugEnabled) debug(message(), err)
}

internal inline fun Logger.info(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isInfoEnabled) info(message())
}

internal inline fun Logger.info(err: Throwable, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isInfoEnabled) info(message(), err)
}

internal inline fun Logger.warn(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isWarnEnabled) warn(message())
}

internal inline fun Logger.warn(err: Throwable, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isWarnEnabled) warn(message(), err)

}

internal inline fun Logger.error(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isErrorEnabled) error(message())
}

internal inline fun Logger.error(err: Throwable, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isErrorEnabled) error(message(), err)
}

internal inline fun Logger.lifecycle(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isLifecycleEnabled) lifecycle(message())
}

internal inline fun Logger.lifecycle(err: Throwable, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isLifecycleEnabled) lifecycle(message(), err)
}

internal inline fun Logger.quiet(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isQuietEnabled) quiet(message())
}

internal inline fun Logger.quiet(err: Throwable, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (isQuietEnabled) quiet(message(), err)
}