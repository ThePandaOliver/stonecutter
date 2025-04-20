package dev.kikugie.stonecutter.data.build

import dev.kikugie.stonecutter.then
import kotlin.collections.MutableMap.MutableEntry

internal inline fun <K : Any, V : Any> newParameterMap(
    delegate: MutableMap<K, V> = mutableMapOf(),
    crossinline keyCheck: (K) -> Unit = {},
    crossinline valueCheck: (V) -> Unit = {},
) = object : ParameterMap<K, V>(delegate) {
    override fun checkKey(key: K) = keyCheck(key)
    override fun checkValue(value: V) = valueCheck(value)
}

internal abstract class ParameterMap<K : Any, V : Any>(private val delegate: MutableMap<K, V> = mutableMapOf()) : MutableMap<K, V> by delegate {
    protected abstract fun checkKey(key: K)
    protected abstract fun checkValue(value: V)

    private inner class ProxyEntry(val delegate: MutableEntry<K, V>) : MutableEntry<K, V> by delegate {
        override fun setValue(newValue: V): V {
            checkValue(newValue)
            return delegate.setValue(newValue)
        }
    }

    override val entries: MutableSet<MutableEntry<K, V>>
        get() = object : MutableSet<MutableEntry<K, V>> by delegate.entries {
            override fun add(element: MutableEntry<K, V>): Boolean =
                if (element in delegate.entries) false
                else put(element.key, element.value) then true

            override fun addAll(elements: Collection<MutableEntry<K, V>>): Boolean {
                var added = false
                for (element in elements) {
                    added = add(element) || added
                }
                return added
            }

            override fun iterator(): MutableIterator<MutableEntry<K, V>> = object : MutableIterator<MutableEntry<K, V>> {
                val iterator = delegate.entries.iterator()
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): MutableEntry<K, V> = ProxyEntry(iterator.next())
                override fun remove() = iterator.remove()
            }
        }
    override val keys: MutableSet<K>
        get() = object : MutableSet<K> by delegate.keys {
            override fun add(element: K): Boolean = reportAddCall()
            override fun addAll(elements: Collection<K>): Boolean = reportAddCall()

            private fun reportAddCall(): Nothing {
                throw UnsupportedOperationException("Can't add keys with null values")
            }
        }
    override val values: MutableCollection<V> = object : MutableCollection<V> by delegate.values {
        override fun iterator(): MutableIterator<V> = object : MutableIterator<V> {
            val iterator = delegate.values.iterator()
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): V = iterator.next()
            override fun remove(): Unit = reportRemoveCall()
        }

        override fun clear(): Unit = reportRemoveCall()
        override fun retainAll(elements: Collection<V>): Boolean = reportRemoveCall()
        override fun removeAll(elements: Collection<V>): Boolean = reportRemoveCall()
        override fun remove(element: V): Boolean = reportRemoveCall()
        override fun addAll(elements: Collection<V>): Boolean = reportAddCall()
        override fun add(element: V): Boolean = reportAddCall()

        private fun reportAddCall(): Nothing {
            throw UnsupportedOperationException("Can't add values with null keys")
        }

        private fun reportRemoveCall(): Nothing {
            throw UnsupportedOperationException("Can't unassign values independently from keys")
        }
    }

    override fun put(key: K, value: V): V? {
        checkKey(key)
        checkValue(value)
        return delegate.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) {
            checkKey(key)
            checkValue(value)
        }
        delegate.putAll(from)
    }
}