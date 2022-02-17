package org.ametro.ui.navigation.entities

import android.graphics.drawable.Drawable

class NavigationHeader(
    val icon: Drawable?,
    val city: String,
    val country: String?,
    val comment: String?,
    val transportTypeIcons: Array<Drawable>
) : NavigationItem() {

    constructor(emptyText: String) : this(null, emptyText, null, null, emptyArray())
}