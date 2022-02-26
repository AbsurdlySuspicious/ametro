package org.ametro.ui.bottom_panel

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import org.ametro.databinding.WidgetMapBottomPanelBinding

interface PanelAdapterBinder<B: ViewBinding> {
    fun createPanel(bind: B)
    fun attachItem()
    fun detachItem() {}
}

class BottomPanelController(binding: WidgetMapBottomPanelBinding) {
    private val routeBinding = binding.includeBotRoute
    private val stationBinding = binding.includeBotStation

    var showRoute: Boolean = false
        set(value) {
            panelShowHide(routeBinder, routeBinding, field, value)
            field = value
        }
    var routeBinder: MapBottomPanelRoute? = null
        set(value) {
            value?.createPanel(routeBinding)
            field = value
        }

    var showStation: Boolean = false
        set(value) {
            panelShowHide(stationBinder, stationBinding, field, value)
            field = value
        }
    var stationBinder: MapBottomPanelStation? = null
        set(value) {
            value?.createPanel(stationBinding)
            field = value
        }

    private val viewOrder: List<ViewGroup> = listOf(
        routeBinding.root,
        stationBinding.root
    )

    init {
        viewOrder.forEach { it.isVisible = false }
    }

    private fun panelShowHide(
        binder: PanelAdapterBinder<*>?,
        panelBinding: ViewBinding,
        prevState: Boolean,
        state: Boolean
    ) {
        if (state)
            binder?.attachItem()
        else
            binder?.detachItem()
        panelBinding.root.isVisible = state
    }

    fun topmostLayout(): ViewGroup? =
        viewOrder.find { it.isVisible }

    fun nonTopLayouts(): List<ViewGroup> =
        viewOrder
            .asSequence()
            .filter { it.visibility != View.GONE }
            .drop(1)
            .toList()

    fun topmostHeight(): Int =
        topmostLayout()?.height ?: 0
}
