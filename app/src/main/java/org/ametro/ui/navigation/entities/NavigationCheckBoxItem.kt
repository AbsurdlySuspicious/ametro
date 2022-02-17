package org.ametro.ui.navigation.entities

import org.ametro.ui.navigation.adapter.HolderFactory
import org.ametro.ui.navigation.adapter.NavigationCheckBoxItemHolderFactory

class NavigationCheckBoxItem(action: Int, val text: CharSequence, var isChecked: Boolean, source: Any?) :
    NavigationItem(action, true, source) {
    override val factory = NavigationCheckBoxItemHolderFactory
}