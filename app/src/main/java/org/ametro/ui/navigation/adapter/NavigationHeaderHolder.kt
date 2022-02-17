package org.ametro.ui.navigation.adapter

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.ametro.R
import org.ametro.app.Constants
import org.ametro.ui.navigation.entities.NavigationHeader
import org.ametro.ui.navigation.entities.NavigationItem
import org.ametro.utils.misc.UIUtils

internal object NavigationHeaderHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.drawer_header_item
    override fun spawnHolder(convertView: View): Holder = NavigationHeaderHolder(convertView)
    override fun onCreateView(view: View) {
        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            val topInset = UIUtils.makeTopInsetsApplier(view)
            view.setOnApplyWindowInsetsListener { _, insets ->
                topInset(insets)
                insets
            }
            UIUtils.requestApplyInsetsWhenAttached(view)
        }
    }
}

internal class NavigationHeaderHolder(view: View) : Holder {
    private val icon: ImageView
    private val city: TextView
    private val country: TextView
    private val comment: TextView
    private val transportsContainer: ViewGroup

    init {
        icon = view.findViewById(R.id.icon)
        city = view.findViewById(R.id.city)
        country = view.findViewById(R.id.country)
        comment = view.findViewById(R.id.comment)
        transportsContainer = view.findViewById(R.id.icons)
    }

    override fun update(item: NavigationItem) {
        val header = item as NavigationHeader
        icon.visibility = if (header.icon == null) View.INVISIBLE else View.VISIBLE
        icon.setImageDrawable(header.icon)
        city.text = header.city
        country.text = header.country
        comment.text = header.comment
        transportsContainer.removeAllViews()
        for (icon in header.transportTypeIcons) {
            val img = ImageView(transportsContainer.context)
            img.setImageDrawable(icon)
            transportsContainer.addView(img)
        }
    }
}