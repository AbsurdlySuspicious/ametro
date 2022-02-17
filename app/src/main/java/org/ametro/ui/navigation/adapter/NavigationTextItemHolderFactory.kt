package org.ametro.ui.navigation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ametro.R

internal class NavigationTextItemHolderFactory : HolderFactory {
    override fun createHolder(convertView: View): Holder {
        val holder = NavigationTextItemHolder(convertView)
        convertView.tag = holder
        return holder
    }

    override fun createView(inflater: LayoutInflater, parent: ViewGroup?): View? {
        return inflater.inflate(R.layout.drawer_text_item, parent, false)
    }
}