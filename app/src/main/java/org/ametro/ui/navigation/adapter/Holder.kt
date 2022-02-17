package org.ametro.ui.navigation.adapter

import org.ametro.ui.navigation.entities.NavigationItem

internal interface Holder {
    fun update(item: NavigationItem)
}