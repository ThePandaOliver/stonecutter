package dev.kikugie.stwotcher.util

import kotlin.math.max
import kotlin.math.min

infix fun Int.extend(len: Int) = this..<(this + len)

infix fun IntRange.shiftBy(n: Int) = (first + n)..(last + n)
infix fun IntRange.merge(other: IntRange) = min(first, other.first)..max(last, other.last)