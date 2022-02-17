package org.ametro.ui.navigation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

internal interface HolderFactory {
    @LayoutRes fun layoutRes(): Int
    fun spawnHolder(convertView: View): Holder
    fun onCreateView(view: View) {}

    fun createView(inflater: LayoutInflater, parent: ViewGroup?): View? {
        val view = inflater.inflate(layoutRes(), parent, false)
        onCreateView(view)
        return view
    }

    fun createHolder(convertView: View): Holder {
        val holder = spawnHolder(convertView)
        convertView.tag = holder
        return holder
    }
}