package org.ametro.ui.navigation.adapter

import android.os.Build
import android.view.View
import android.widget.TextView
import org.ametro.R
import org.ametro.app.Constants
import org.ametro.ui.navigation.entities.NavigationItem
import org.ametro.ui.navigation.entities.NavigationSubHeader
import org.ametro.utils.misc.UIUtils

internal object NavigationPaddingHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.empty_layout
    override fun spawnHolder(convertView: View): Holder = NavigationPaddingHolder(convertView)
}

internal class NavigationPaddingHolder(view: View) : Holder {
    init {
        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            val insetApplier = UIUtils.makeBottomInsetsApplier(view)
            view.setOnApplyWindowInsetsListener { _, insets ->
                insetApplier.applyInset(insets)
                insets
            }
            UIUtils.requestApplyInsetsWhenAttached(view)
        }
    }

    override fun update(item: NavigationItem) {}
}