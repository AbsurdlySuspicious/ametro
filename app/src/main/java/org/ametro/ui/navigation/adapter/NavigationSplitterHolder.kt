package org.ametro.ui.navigation.adapter

import android.view.View
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationItem

internal object NavigationSplitterHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.drawer_splitter_item
    override fun spawnHolder(convertView: View): Holder = NavigationSplitterHolder()
}

internal class NavigationSplitterHolder : Holder {
    override fun update(item: NavigationItem) {}
}