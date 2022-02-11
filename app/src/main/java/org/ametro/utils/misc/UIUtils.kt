package org.ametro.utils.misc

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.view.updatePadding

object UIUtils {
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun makeTopInsetsApplier(view: View): (WindowInsets) -> Unit {
        val viewHeight = view.layoutParams.height
        val viewPadding = view.paddingTop
        return { insets ->
            val topInset = insets.systemWindowInsetTop
            view.layoutParams.height = viewHeight + topInset
            view.updatePadding(top = viewPadding + topInset)
        }
    }

    @ColorInt
    fun colorArgb(alpha: Float, red: Float, green: Float, blue: Float): Int {
        return (alpha * 255.0f + 0.5f).toInt() shl 24 or
                ((red * 255.0f + 0.5f).toInt() shl 16) or
                ((green * 255.0f + 0.5f).toInt() shl 8) or
                (blue * 255.0f + 0.5f).toInt()
    }

}