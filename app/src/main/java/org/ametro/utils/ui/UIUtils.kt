package org.ametro.utils.ui

import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toolbar
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.ametro.app.Constants


abstract class InsetsApplier(val view: View, val initialPadding: Int, val keepHeight: Boolean = false) {
    val initialHeight = view.layoutParams.height

    protected abstract fun updatePadding(padding: Int)
    abstract fun getInset(insets: WindowInsetsCompat): Int

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun applyInset(insets: WindowInsets) {
        val inset = getInset(WindowInsetsCompat.toWindowInsetsCompat(insets))
        if (!keepHeight) view.layoutParams.height = initialHeight + inset
        updatePadding(initialPadding + inset)
    }
}

fun makeTopInsetsApplier(view: View) =
    object : InsetsApplier(view, view.paddingTop) {
        override fun updatePadding(padding: Int) =
            view.updatePadding(top = padding)

        override fun getInset(insets: WindowInsetsCompat) =
            insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
    }

fun makeBottomInsetsApplier(view: View, keepHeight: Boolean = false) =
    object : InsetsApplier(view, view.paddingBottom, keepHeight) {
        override fun updatePadding(padding: Int) =
            view.updatePadding(bottom = padding)

        override fun getInset(insets: WindowInsetsCompat): Int {
            val mask = WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.systemGestures()
            return insets.getInsets(mask).bottom
        }
    }

fun applyInsets(
    insetsApplier: InsetsApplier,
    additionalActions: (WindowInsets) -> Unit = {}
) {
    if (Build.VERSION.SDK_INT < Constants.INSETS_MIN_API)
        return
    insetsApplier.view.setOnApplyWindowInsetsListener { _, insets ->
        insetsApplier.applyInset(insets)
        additionalActions(insets)
        insets
    }
    requestApplyInsetsWhenAttached(insetsApplier.view)
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
