package org.ametro.ui.navigation.entities

import org.ametro.ui.navigation.adapter.NavigationPaddingHolderFactory

class NavigationPaddingItem : NavigationItem(INVALID_ACTION) {
    override val factory = NavigationPaddingHolderFactory
}