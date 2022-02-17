package org.ametro.ui.navigation

import android.view.View
import org.ametro.model.entities.MapDelay

interface NavigationControllerListener {
    fun onOpenMaps(): Boolean
    fun onOpenSettings(): Boolean
    fun onChangeScheme(schemeName: String): Boolean
    fun onToggleTransport(source: String, checked: Boolean): Boolean
    fun onDelayChanged(delay: MapDelay): Boolean
    fun onOpenAbout(): Boolean
    fun onDrawerSlide(drawerView: View, slideOffset: Float)
}