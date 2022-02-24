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
            val newHeight = viewHeight + topInset
            view.layoutParams.height = newHeight
            view.updatePadding(top = viewPadding + topInset)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun requestApplyInsetsWhenAttached(view: View) {
        if (view.isAttachedToWindow) {
            view.requestApplyInsets()
        } else {
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    v.requestApplyInsets()
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })
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