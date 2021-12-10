package org.ametro.utils.misc

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
