package org.ametro.utils.misc

import android.util.Pair

fun <A, B> convertPair(p: kotlin.Pair<A, B>): Pair<A, B> {
    return Pair(p.first, p.second)
}

fun <A, B> convertPair(p: Pair<A, B>): kotlin.Pair<A, B> {
    return kotlin.Pair(p.first, p.second)
}

inline fun <T, reified R> Array<T>.mapArray(noinline mapper: (T) -> R): Array<R> {
    val out = arrayOfNulls<R>(this.size)
    this.asSequence()
        .map(mapper)
        .withIndex()
        .forEach { (i, r) -> out[i] = r }
    @Suppress("UNCHECKED_CAST")
    return out as Array<R>
}

inline fun <T, reified R> Array<T>.mapArrayIndexed(noinline mapper: (IndexedValue<T>) -> R): Array<R> {
    val out = arrayOfNulls<R>(this.size)
    this.asSequence()
        .withIndex()
        .map(mapper)
        .withIndex()
        .forEach { (i, r) -> out[i] = r }
    @Suppress("UNCHECKED_CAST")
    return out as Array<R>
}

inline fun <T, reified R> Iterable<T>.mapToArraySizedExact(size: Int, noinline mapper: (T) -> R): Array<R> {
    val out = arrayOfNulls<R>(size)
    var total = 0
    this.asSequence()
        .map(mapper)
        .withIndex()
        .forEach { (i, r) ->
            total++
            out[i] = r
        }
    if (total != size)
        throw IndexOutOfBoundsException("Iterable size $total is not equal to defined size $size")
    @Suppress("UNCHECKED_CAST")
    return out as Array<R>
}
