package org.ametro.utils.misc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Pair
import kotlin.math.abs
import kotlin.random.Random

const val EPSILON = 1.19209290e-07f

fun epsilonEqual(a: Float, b: Float): Boolean =
    a == b || abs(a - b) < EPSILON

fun saturate(value: Float, minFrom: Float, minTo: Float, maxFrom: Float, maxTo: Float): Float =
    if (value > maxFrom) maxTo
    else if (value < minFrom) minTo
    else value

fun <A, B> convertPair(p: kotlin.Pair<A, B>): Pair<A, B> {
    return Pair(p.first, p.second)
}

fun <A, B> convertPair(p: Pair<A, B>): kotlin.Pair<A, B> {
    return kotlin.Pair(p.first, p.second)
}

private val schemaRe = Regex("^[a-zA-Z]+://.+$")

fun linkIntent(url: String, forceHttp: Boolean = false) =
    Intent(Intent.ACTION_VIEW).also {
        var fixedUrl = url
        if (forceHttp && !schemaRe.matches(fixedUrl))
            fixedUrl = "http://$fixedUrl"
        it.data = Uri.parse(fixedUrl)
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

fun getAppVersion(context: Context) = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    ""
}
