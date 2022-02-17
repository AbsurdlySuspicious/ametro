package org.ametro.ui.navigation.adapter

import android.view.View
import android.widget.TextView
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationItem
import org.ametro.ui.navigation.entities.NavigationSubHeader

internal object NavigationSubHeaderHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.drawer_sub_header_item
    override fun spawnHolder(convertView: View): Holder = NavigationSubHeaderHolder(convertView)
}

internal class NavigationSubHeaderHolder(view: View) : Holder {
    private val textView: TextView

    init {
        textView = view.findViewById(R.id.text)
    }

    override fun update(item: NavigationItem) {
        val textItem = item as NavigationSubHeader
        textView.text = textItem.text
    }
}