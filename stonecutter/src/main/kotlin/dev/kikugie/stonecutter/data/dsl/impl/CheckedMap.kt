package dev.kikugie.stonecutter.data.dsl.impl

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
internal abstract class CheckedMutableSet<E : Any>(val delegate: MutableSet<E> = mutableSetOf()) : MutableSet<E> by delegate {
    @Throws open fun checkElement(element: E) {}

    override fun add(element: E): Boolean {
        checkElement(element)
        return delegate.add(element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        for (element in elements) checkElement(element)
        return delegate.addAll(elements)
    }
}

internal abstract class CheckedMutableMap<K : Any, V : Any>(val delegate: MutableMap<K, V> = mutableMapOf()) : MutableMap<K, V> by delegate {
    @Throws open fun checkKey(key: K) {}
    @Throws open fun checkValue(value: V) {}

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = object : MutableSet<MutableMap.MutableEntry<K, V>> by delegate.entries {
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            val iterator = delegate.entries.iterator()
            return object : MutableIterator<MutableMap.MutableEntry<K, V>> by iterator {
                override fun next(): MutableMap.MutableEntry<K, V> = iterator.next().asChecked()
            }
        }
    }

    override fun put(key: K, value: V): V? {
        checkKey(key); checkValue(value)
        return delegate.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) { checkKey(key); checkValue(value) }
        delegate.putAll(from)
    }

    private fun MutableMap.MutableEntry<K, V>.asChecked(): MutableMap.MutableEntry<K, V> = object : MutableMap.MutableEntry<K, V> by this {
        override fun setValue(newValue: V): V {
            checkValue(newValue)
            return this@asChecked.setValue(newValue)
        }
    }
}