package org.ametro.ui.navigation.adapter

import org.ametro.ui.navigation.adapter.IHolderFactory
import org.ametro.ui.navigation.adapter.IHolder
import org.ametro.ui.navigation.adapter.NavigationHeaderHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import org.ametro.R
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.view.updatePadding
import org.ametro.app.Constants
import org.ametro.utils.misc.UIUtils

internal class NavigationHeaderHolderFactory : IHolderFactory {
    override fun createHolder(convertView: View): IHolder {
        val holder = NavigationHeaderHolder(convertView)
        convertView.tag = holder
        return holder
    }

    override fun createView(inflater: LayoutInflater, parent: ViewGroup): View {
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