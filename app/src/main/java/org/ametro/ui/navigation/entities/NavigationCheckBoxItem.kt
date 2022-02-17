package org.ametro.ui.navigation.entities

class NavigationCheckBoxItem(action: Int, val text: CharSequence, var isChecked: Boolean, source: Any?) :
    NavigationItem(action, true, source)