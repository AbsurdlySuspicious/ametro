package org.ametro.ui.navigation.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationItem
import org.ametro.ui.navigation.entities.NavigationTextItem

internal object NavigationTextItemHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.drawer_text_item
    override fun spawnHolder(convertView: View): Holder = NavigationTextItemHolder(convertView)
}

internal class NavigationTextItemHolder(view: View) : Holder {
    private val imageView: ImageView
    private val textView: TextView
    private val container: View

    init {
        imageView = view.findViewById(R.id.icon)
        textView = view.findViewById(R.id.text)
        container = view
    }

    override fun update(item: NavigationItem) {
        val textItem = item as NavigationTextItem
        imageView.setImageDrawable(textItem.drawable)
        textView.text = textItem.text
        container.setBackgroundResource(if (textItem.isSelected) R.color.activated_color else android.R.color.transparent)
    }
}