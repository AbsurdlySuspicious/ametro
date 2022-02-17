package org.ametro.ui.navigation.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationHeader
import org.ametro.ui.navigation.entities.NavigationItem

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