package org.ametro.ui.navigation.adapter

import android.view.View
import androidx.core.view.updatePadding
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationItem
import org.ametro.ui.navigation.entities.NavigationPaddingItem

internal object NavigationPaddingHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.drawer_padding_item
    override fun spawnHolder(convertView: View): Holder = NavigationPaddingHolder(convertView)
}

internal class NavigationPaddingHolder(val view: View) : Holder {
    override fun update(item: NavigationItem) {
        val item = item as NavigationPaddingItem
        view.layoutParams.height = item.height
        view.requestLayout()
    }
}