package org.ametro.ui.navigation.entities

import org.ametro.ui.navigation.adapter.HolderFactory
import org.ametro.ui.navigation.adapter.NavigationSubHeaderHolderFactory

class NavigationSubHeader @JvmOverloads constructor(
    val text: CharSequence,
    override val items: Array<NavigationItem>,
    tag: String? = null
) : NavigationItem(INVALID_ACTION), NavigationItemGroup {
    override val factory = NavigationSubHeaderHolderFactory

    init {
        this.tag = tag
    }
}