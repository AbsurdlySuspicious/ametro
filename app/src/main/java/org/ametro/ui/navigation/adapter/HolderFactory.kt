package org.ametro.ui.navigation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

internal interface HolderFactory {
    fun createView(inflater: LayoutInflater, parent: ViewGroup?): View?
    fun createHolder(view: View): Holder
}