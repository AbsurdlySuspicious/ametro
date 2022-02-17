package org.ametro.ui.navigation.adapter

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import org.ametro.R
import org.ametro.ui.navigation.entities.NavigationCheckBoxItem
import org.ametro.ui.navigation.entities.NavigationItem

internal object NavigationCheckBoxItemHolderFactory : HolderFactory {
    override fun layoutRes(): Int = R.layout.drawer_checkbox_item
    override fun spawnHolder(convertView: View): Holder = NavigationCheckBoxItemHolder(convertView)
}

internal class NavigationCheckBoxItemHolder(view: View) : Holder {
    private val checkBox: CheckBox
    private val textView: TextView

    init {
        checkBox = view.findViewById(R.id.checkbox)
        textView = view.findViewById(R.id.text)
    }

    override fun update(item: NavigationItem) {
        val checkboxItem = item as NavigationCheckBoxItem
        checkBox.isChecked = checkboxItem.isChecked
        textView.text = checkboxItem.text
    }
}