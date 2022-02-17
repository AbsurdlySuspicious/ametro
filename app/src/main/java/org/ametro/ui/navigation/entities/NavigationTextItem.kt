package org.ametro.ui.navigation.entities

import android.graphics.drawable.Drawable

class NavigationTextItem @JvmOverloads constructor(
    action: Int,
    val drawable: Drawable?,
    val text: CharSequence,
    enabled: Boolean = true,
    source: Any? = null
) : NavigationItem(action, enabled, source)