package org.ametro.ui.navigation.entities

import android.graphics.drawable.Drawable
import org.ametro.ui.navigation.adapter.HolderFactory
import org.ametro.ui.navigation.adapter.NavigationHeaderHolderFactory

class NavigationHeader(
    val icon: Drawable?,
    val city: String,
    val country: String?,
    val comment: String?,
    val transportTypeIcons: Array<Drawable>
) : NavigationItem() {
    override val factory = NavigationHeaderHolderFactory
    constructor(emptyText: String) : this(null, emptyText, null, null, emptyArray())
}