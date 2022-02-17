package org.ametro.ui.navigation.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ametro.R
import org.ametro.app.Constants
import org.ametro.utils.misc.UIUtils

internal class NavigationHeaderHolderFactory : HolderFactory {
    override fun createHolder(convertView: View): Holder {
        val holder = NavigationHeaderHolder(convertView)
        convertView.tag = holder
        return holder
    }

    override fun createView(inflater: LayoutInflater, parent: ViewGroup?): View {
        val view = inflater.inflate(R.layout.drawer_header_item, parent, false)
        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            val topInset = UIUtils.makeTopInsetsApplier(view)
            view.setOnApplyWindowInsetsListener { _, insets ->
                topInset(insets)
                insets
            }
            UIUtils.requestApplyInsetsWhenAttached(view)
        }
        return view
    }
}