package org.ametro.ui.navigation.entities

class NavigationSubHeader @JvmOverloads constructor(
    val text: CharSequence,
    override val items: Array<NavigationItem>,
    tag: String? = null
) : NavigationItem(NavigationItem.Companion.INVALID_ACTION), NavigationItemGroup {

    init {
        this.tag = tag
    }
}