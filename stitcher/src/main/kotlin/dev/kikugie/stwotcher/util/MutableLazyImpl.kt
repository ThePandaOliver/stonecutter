package dev.kikugie.stwotcher.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> mutableLazy(init: () -> T): ReadWriteProperty<Any?, T> = MutableLazyImpl(init)

class MutableLazyImpl<T>(init: () -> T) : ReadWriteProperty<Any?, T> {
    private object UninitializedValue
    private var initializer: (() -> T)? = init
    private var value: Any? = UninitializedValue

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value === UninitializedValue) setValue(thisRef, property, initializer!!())
        return value as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, new: T) {
        value = new
        initializer = null
    }
}