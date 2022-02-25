package org.ametro.ui.bottom_panel

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import org.ametro.app.Constants
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.utils.misc.UIUtils

interface PanelAdapterBinder<B : ViewBinding> {
    fun createPanel(bind: B)
    fun attachItem()
    fun detachItem() {}
}

class BottomPanelController(binding: WidgetMapBottomPanelBinding) {
    private val routeBinding = binding.includeBotRoute
    private val stationBinding = binding.includeBotStation
    private var insets: WindowInsets? = null

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

    private val viewOrder = makeViewOrder(
        routeBinding.root,
        stationBinding.root
    )

    private data class BottomPanelItem(
        val view: ViewGroup,
        val insetsApplier: UIUtils.InsetsApplier?
    )

    private fun makeViewOrder(vararg views: ViewGroup): List<BottomPanelItem> {
        return views.map {
            val insetsApplier =
                UIUtils.makeBottomInsetsApplier(it, keepHeight = true)
            BottomPanelItem(it, insetsApplier)
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Constants.INSETS_MIN_API) {
            binding.root.setOnApplyWindowInsetsListener { _, insets ->
                this.insets = insets
                insets
            }
            UIUtils.requestApplyInsetsWhenAttached(binding.root)
        }
        viewOrder.forEach { it.view.isVisible = false }
    }

    private fun applyInsets(item: BottomPanelItem?) {
        if (Build.VERSION.SDK_INT < Constants.INSETS_MIN_API) return
        viewOrder.forEach { it.insetsApplier?.rollback() }
        item?.insetsApplier?.applyInset(insets ?: return)
    }

    fun applyInsetsTopmost() {
        applyInsets(topmostItem())
    }

    fun applyInsetsBottomest() {
        applyInsets(bottomestItem())
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

    private fun bottomestItem(): BottomPanelItem? =
        viewOrder.findLast { it.view.isVisible }

    private fun topmostItem(): BottomPanelItem? =
        viewOrder.find { it.view.isVisible }

    fun topmostLayout(): ViewGroup? =
        topmostItem()?.view

    fun topmostHeight(): Int =
        topmostLayout()?.height ?: 0
}
