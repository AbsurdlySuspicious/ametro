package org.ametro.ui.navigation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ametro.R

internal class NavigationSplitterHolderFactory : HolderFactory {
    override fun createHolder(convertView: View): Holder {
        val holder = NavigationSplitterHolder()
        convertView.tag = holder
        return holder
    }

    override fun createView(inflater: LayoutInflater, parent: ViewGroup?): View? {
        return inflater.inflate(R.layout.drawer_splitter_item, parent, false)
    }
}