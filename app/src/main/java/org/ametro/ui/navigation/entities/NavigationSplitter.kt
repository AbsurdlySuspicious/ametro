package org.ametro.ui.navigation.entities

import org.ametro.ui.navigation.adapter.NavigationSplitterHolderFactory

class NavigationSplitter : NavigationItem(0) {
    override val factory = NavigationSplitterHolderFactory
}