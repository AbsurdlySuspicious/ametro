package org.ametro.ui.navigation.adapter

import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationItem
import org.ametro.ui.navigation.entities.NavigationTextItem

internal object NavigationTextItemHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.drawer_text_item
    override fun spawnHolder(convertView: View): Holder = NavigationTextItemHolder(convertView)
}

internal class NavigationTextItemHolder(view: View) : Holder {
    private val imageView: ImageView = view.findViewById(R.id.icon)
    private val textView: TextView = view.findViewById(R.id.text)
    private val bg: View = view.findViewById(R.id.bg)

    private val activatedColor =
        ResourcesCompat.getColor(view.context.resources, R.color.activated_color, null)
    private val noIcon =
        ResourcesCompat.getDrawable(view.context.resources, R.drawable.ic_unknown_item2, null)

    override fun update(item: NavigationItem) {
        val textItem = item as NavigationTextItem
        imageView.setImageDrawable(textItem.drawable ?: noIcon)
        textView.text = textItem.text

        val filter = if (item.isSelected) PorterDuff.Mode.DARKEN else PorterDuff.Mode.DST_IN
        bg.background.setColorFilter(activatedColor, filter)
    }
}