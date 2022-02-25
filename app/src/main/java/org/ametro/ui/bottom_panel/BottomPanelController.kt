package org.ametro.ui.bottom_panel

import android.os.Build
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import org.ametro.app.Constants
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.utils.misc.UIUtils

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
        viewOrder.forEach {
            if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
                val insetsApplier = UIUtils.makeBottomInsetsApplier(it, keepHeight = true)
                it.setOnApplyWindowInsetsListener { _, insets ->
                    insetsApplier.applyInset(insets)
                    insets
                }
                UIUtils.requestApplyInsetsWhenAttached(it)
            }
            it.isVisible = false
        }
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

    fun topmostHeight(): Int =
        topmostLayout()?.height ?: 0
}
