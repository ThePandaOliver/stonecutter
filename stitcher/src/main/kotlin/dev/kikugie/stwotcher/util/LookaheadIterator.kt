package dev.kikugie.stwotcher.util

fun <T> LookaheadIterator<T>.filter(selector: (T) -> Boolean): LookaheadIterator<T> where T : Any =
    FilteringLookaheadIterator<T>(this, selector)

/**Iterator that allows seeing the next element without advancing it.*/
interface LookaheadIterator<T> : Iterator<T> where T : Any {
    fun peek(): T?
}

class FilteringLookaheadIterator<T>(private val delegate: LookaheadIterator<T>, private val filter: (T) -> Boolean) : LookaheadIterator<T> where T : Any {
    private var next: T? by mutableLazy(::findNext)

    override fun hasNext(): Boolean = next != null
    override fun peek(): T? = next
    override fun next(): T = next?.also { next = findNext() }
        ?: throw NoSuchElementException()

    private fun findNext(): T? {
        while (delegate.hasNext()) {
            val next = delegate.next()
            if (filter(next)) return next
        }
        return null
    }
}

interface LookaheadIterable<T> : Iterable<T> where T : Any {
    override fun iterator(): LookaheadIterator<T>
}