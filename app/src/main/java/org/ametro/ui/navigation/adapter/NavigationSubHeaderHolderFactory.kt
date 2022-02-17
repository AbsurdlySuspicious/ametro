package org.ametro.ui.navigation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ametro.R

internal class NavigationSubHeaderHolderFactory : HolderFactory {
    override fun createHolder(convertView: View): Holder {
        val holder = NavigationSubHeaderHolder(convertView)
        convertView.tag = holder
        return holder
    }

    override fun createView(inflater: LayoutInflater, parent: ViewGroup?): View? {
        return inflater.inflate(R.layout.drawer_sub_header_item, parent, false)
    }
}