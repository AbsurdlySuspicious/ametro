package org.ametro.ui.navigation.entities

import org.ametro.ui.navigation.adapter.HolderFactory

abstract class NavigationItem @JvmOverloads constructor(
    val action: Int = INVALID_ACTION,
    var isEnabled: Boolean = false,
    var isSelected: Boolean = false,
    val source: Any? = null
) {
    internal abstract val factory: HolderFactory

    var tag: String? = null
        protected set

    constructor(action: Int, enabled: Boolean, source: Any?) : this(action, enabled, false, source)

    companion object {
        const val INVALID_ACTION = -1
    }
}