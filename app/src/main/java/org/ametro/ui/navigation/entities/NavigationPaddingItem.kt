package org.ametro.ui.navigation.entities

import org.ametro.ui.navigation.adapter.NavigationPaddingHolderFactory

class NavigationPaddingItem(val height: Int) : NavigationItem(INVALID_ACTION) {
    override val factory = NavigationPaddingHolderFactory
}