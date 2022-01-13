package org.ametro.utils.misc

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import androidx.annotation.ColorInt
import kotlin.math.pow
import kotlin.math.roundToInt

object AnimUtils {
    inline fun getValueAnimator(
        forward: Boolean = true,
        duration: Long,
        interpolator: TimeInterpolator,
        crossinline updateListener: (progress: Float) -> Unit
    ): ValueAnimator {
        val a =
            if (forward) ValueAnimator.ofFloat(0f, 1f)
            else ValueAnimator.ofFloat(1f, 0f)
        a.addUpdateListener { updateListener(it.animatedValue as Float) }
        a.duration = duration
        a.interpolator = interpolator
        return a
    }

    @ColorInt
    fun argbEvaluate(fraction: Float, @ColorInt startValue: Int, @ColorInt endValue: Int): Int {
        val startA = (startValue shr 24 and 0xff) / 255.0f
        var startR = (startValue shr 16 and 0xff) / 255.0f
        var startG = (startValue shr 8 and 0xff) / 255.0f
        var startB = (startValue and 0xff) / 255.0f
        val endA = (endValue shr 24 and 0xff) / 255.0f
        var endR = (endValue shr 16 and 0xff) / 255.0f
        var endG = (endValue shr 8 and 0xff) / 255.0f
        var endB = (endValue and 0xff) / 255.0f

        // convert from sRGB to linear
        startR = startR.toDouble().pow(2.2).toFloat()
        startG = startG.toDouble().pow(2.2).toFloat()
        startB = startB.toDouble().pow(2.2).toFloat()
        endR = endR.toDouble().pow(2.2).toFloat()
        endG = endG.toDouble().pow(2.2).toFloat()
        endB = endB.toDouble().pow(2.2).toFloat()

        // compute the interpolated color in linear space
        var a = startA + fraction * (endA - startA)
        var r = startR + fraction * (endR - startR)
        var g = startG + fraction * (endG - startG)
        var b = startB + fraction * (endB - startB)

        // convert back to sRGB in the [0..255] range
        a *= 255.0f
        r = r.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f
        g = g.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f
        b = b.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f
        return a.roundToInt() shl 24 or
                (r.roundToInt() shl 16) or
                (g.roundToInt() shl 8) or
                b.roundToInt()
    }

}
