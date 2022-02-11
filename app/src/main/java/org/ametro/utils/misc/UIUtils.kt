package org.ametro.utils.misc

import android.os.Build
import android.view.View
import android.view.WindowInsets
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
}