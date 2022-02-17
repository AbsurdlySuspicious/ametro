package org.ametro.ui.navigation.adapter

import android.view.View
import android.widget.TextView
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationItem
import org.ametro.ui.navigation.entities.NavigationSubHeader

internal class NavigationSubHeaderHolder(view: View) : Holder {
    private val textView: TextView

    init {
        textView = view.findViewById(R.id.text)
    }

    override fun update(item: NavigationItem) {
        val textItem = item as NavigationSubHeader
        textView.text = textItem.text
    }
}