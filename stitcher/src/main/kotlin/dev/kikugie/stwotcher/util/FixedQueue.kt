package dev.kikugie.stwotcher.util

import java.util.Queue
import kotlin.collections.isNotEmpty

/**
 * A fixed-size queue implementation that allows elements to be added and removed
 * in a first-in, first-out (FIFO) order up to a predefined capacity. This class
 * does not support removal of arbitrary elements or resizing of capacity.
 *
 * The implementation is tailored for use in [dev.kikugie.stwotcher.exec.scan.CommentScanner],
 * which frequently needs to buffer elements. Unlike [kotlin.collections.ArrayDeque],
 * removing elements doesn't shift the underlying array, resulting in improved performance.
 *
 * @param T The type of elements stored in the queue
 * @property capacity The maximum number of elements the queue can hold
 */
@Suppress("UNCHECKED_CAST")
class FixedQueue<T>(val capacity: Int) : Queue<T> {
    private val array: Array<Any?> = arrayOfNulls(capacity)
    private var tail = 0
    private var head = 0
    override val size: Int get() =
        if (tail >= head) tail - head
        else capacity - (tail - head)

    override fun add(e: T): Boolean =
        if (isNotFull()) addImpl(e)
        else throw IllegalStateException(FULL_EXC)

    override fun offer(e: T): Boolean =
        if (isNotFull()) addImpl(e)
        else false

    override fun remove(): T =
        if (isNotEmpty()) removeImpl()
        else throw NoSuchElementException(EMPTY_EXC)

    override fun poll(): T? =
        if (isNotEmpty()) removeImpl()
        else null

    override fun element(): T =
        if (isNotEmpty()) elementImpl()
        else throw NoSuchElementException(EMPTY_EXC)

    override fun peek(): T? =
        if (isNotEmpty()) elementImpl()
        else null

    override fun addAll(elements: Collection<T>): Boolean {
        require(elements.size <= capacity - size) { "Not enough space to add ${elements.size} elements" }
        elements.forEach(::addImpl)
        return true
    }

    override fun clear() {
        tail = 0
        head = 0
        array.fill(null)
    }

    override fun iterator(): MutableIterator<T> = FixedQueueIterator(array.copyOf(), tail, size)

    override fun remove(element: T?): Boolean {
        throw UnsupportedOperationException(REMOVAL_EXC)
    }

    override fun removeAll(elements: Collection<T?>): Boolean {
        throw UnsupportedOperationException(REMOVAL_EXC)
    }

    override fun retainAll(elements: Collection<T?>): Boolean {
        throw UnsupportedOperationException(REMOVAL_EXC)
    }

    override fun contains(element: T): Boolean =
        array.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean =
        elements.all(::contains)

    override fun isEmpty(): Boolean = size == 0
    fun isFull() = size == capacity
    fun isNotFull() = !isFull()

    private fun addImpl(e: T): Boolean {
        tail = (tail + 1) % capacity
        array[tail] = e
        return true
    }

    private fun removeImpl(): T {
        array[head] = null
        head = (head + 1) % capacity
        return array[head] as T
    }

    private fun elementImpl(): T =
        array[tail] as T

    private class FixedQueueIterator<T>(val array: Array<Any?>, val start: Int = 0, val steps: Int) : MutableIterator<T> {
        var offset = 0

        override fun hasNext(): Boolean =
            offset < steps

        override fun next(): T =
            array[(start + offset++) % array.size] as T

        override fun remove() {
            throw UnsupportedOperationException(REMOVAL_EXC)
        }
    }

    private companion object {
        const val EMPTY_EXC = "Queue is empty"
        const val FULL_EXC = "Queue is full"
        const val REMOVAL_EXC = "Removal of arbitrary elements is not supported"
    }
}