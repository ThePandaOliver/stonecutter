package dev.kikugie.stwotcher.util

inline fun <T> LookaheadIterator<T>.filter(crossinline selector: (T) -> Boolean): LookaheadIterator<T> where T : Any =
    object : FilteringLookaheadIterator<T>(this) {
        override fun accept(element: T): Boolean = selector(element)
    }

/**Iterator that allows seeing the next element without advancing it.*/
interface LookaheadIterator<T> : Iterator<T> where T : Any {
    fun peek(): T?
}

abstract class FilteringLookaheadIterator<T>(private val delegate: LookaheadIterator<T>) : LookaheadIterator<T> where T : Any {
    private var next: T? = findNext()

    abstract fun accept(element: T): Boolean

    override fun hasNext(): Boolean = next != null
    override fun peek(): T? = next
    override fun next(): T = next?.also { next = findNext() }
        ?: throw NoSuchElementException()

    private fun findNext(): T? {
        var next: T?
        do next = delegate.peek()
        while (next != null && !accept(next))
        return next
    }
}

interface LookaheadIterable<T> : Iterable<T> where T : Any {
    override fun iterator(): LookaheadIterator<T>
}