package org.ametro.ui.bottom_panel

import android.util.Log
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import org.ametro.databinding.WidgetMapBottomPanelBinding

interface PanelAdapterBinder {
    fun bindItem(bind: ViewBinding)
    fun createHolder(bind: ViewBinding) {}
    fun attachItem(bind: ViewBinding) {}
    fun detachItem(bind: ViewBinding) {}
}

class BottomPanelController(private val binding: WidgetMapBottomPanelBinding) {
    var showRoute: Boolean = false
        set(value) {
            routeShowHide(field, value)
            field = value
        }
    var routeBinder: PanelAdapterBinder? = null
        set(value) {
            setBinder(field, value, binding.includeBotRoute)
            field = value
        }

    var showStation: Boolean = false
        set(value) {
            stationShowHide(field, value)
            field = value
        }
    var stationBinder: PanelAdapterBinder? = null
        set(value) {
            setBinder(field, value, binding.includeBotStation)
            field = value
        }

    private val routeBinding = binding.includeBotRoute
    private val stationBinding = binding.includeBotStation

    private val viewOrder: List<ViewGroup> = listOf(
        routeBinding.itemBotRoute,
        stationBinding.itemBotStation
    )

    init {
        viewOrder.forEach { it.isVisible = false }
    }

    private fun setBinder(prev: PanelAdapterBinder?, curr: PanelAdapterBinder?, bind: ViewBinding) {
        if (prev != curr && curr != null)
            curr.createHolder(bind)
    }

    private fun <B : ViewBinding> bindPanel(
        bind: B,
        binder: PanelAdapterBinder?,
        state: Boolean
    ) {
        if (state)
            binder?.apply {
                bindItem(bind)
                attachItem(bind)
            }
        else
            binder?.detachItem(bind)
    }

    private fun routeShowHide(prevState: Boolean, state: Boolean) {
        bindPanel(routeBinding, routeBinder, state)
        routeBinding.itemBotRoute.isVisible = state
        if (state) {
            routeBinding.pager.setCurrentItem(0, true)
        }
    }

    private fun stationShowHide(prevState: Boolean, state: Boolean) {
        bindPanel(stationBinding, stationBinder, state)
        stationBinding.itemBotStation.isVisible = state
    }

    fun topmostLayout(): ViewGroup? =
        viewOrder.find { it.isVisible }

    fun topmostHeight(): Int =
        topmostLayout()?.height ?: 0
}
