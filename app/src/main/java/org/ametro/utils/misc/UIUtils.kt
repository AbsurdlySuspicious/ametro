package org.ametro.utils.misc

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

object UIUtils {
    abstract class InsetsApplier(val view: View, val initialPadding: Int) {
        val initialHeight = view.layoutParams.height

        protected abstract fun updatePadding(padding: Int)
        protected abstract fun getInset(insets: WindowInsetsCompat): Int

        @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
        fun applyInset(insets: WindowInsets) {
            val inset = getInset(WindowInsetsCompat.toWindowInsetsCompat(insets))
            view.layoutParams.height = initialHeight + inset
            updatePadding(initialPadding + inset)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun makeTopInsetsApplier(view: View) = object : InsetsApplier(view, view.paddingTop) {
        override fun updatePadding(padding: Int) =
            view.updatePadding(top = padding)
        override fun getInset(insets: WindowInsetsCompat) =
            insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun makeBottomInsetsApplier(view: View) = object : InsetsApplier(view, view.paddingBottom) {
        override fun updatePadding(padding: Int) =
            view.updatePadding(bottom = padding)
        override fun getInset(insets: WindowInsetsCompat) =
            insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
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