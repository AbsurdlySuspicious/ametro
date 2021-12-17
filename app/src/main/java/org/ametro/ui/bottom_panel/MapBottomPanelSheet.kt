package org.ametro.ui.bottom_panel

import android.view.View
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetMapBottomPanelBinding

class MapBottomPanelSheet(
    val sheetView: NestedScrollView,
    val app: ApplicationEx
) {
    companion object {
        const val PENDING_OPEN_NO = 0
        const val PENDING_OPEN_EXPAND = 1
        const val PENDING_OPEN_COLLAPSE = 2

        const val OPENED_IGNORE = 0
        const val OPENED_CHANGE_VIEW = 1
        const val OPENED_REOPEN = 2
    }

    val adapter = BottomPanelAdapter(sheetView.context)
    val bottomSheet = BottomSheetBehavior.from(sheetView)

    private val binding = WidgetMapBottomPanelBinding.bind(sheetView)
    private val recycler = binding.recycler

    private val topPadViews = listOf(binding.drag)

    private var sheetStateCallbacksPre: MutableList<(View, Int) -> Unit> = mutableListOf()
    private var pendingSheetAction: (() -> Unit)? = null

    private fun runPendingSheetAction() {
        pendingSheetAction?.let {
            it()
            pendingSheetAction = null
        }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(sheetView: View, newState: Int) {
            // Log.i("MEME", "Bottom sheet state: ${BottomSheetUtils.stateToString(newState)}")
            sheetStateCallbacksPre
                .forEach { it(sheetView, newState) }
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    runPendingSheetAction()
                    if (pendingOpen != PENDING_OPEN_NO) {
                        bottomSheet.state = when (pendingOpen) {
                            PENDING_OPEN_EXPAND -> BottomSheetBehavior.STATE_EXPANDED
                            PENDING_OPEN_COLLAPSE -> BottomSheetBehavior.STATE_COLLAPSED
                            else -> BottomSheetBehavior.STATE_HIDDEN
                        }
                        pendingOpen = PENDING_OPEN_NO
                    }
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    runPendingSheetAction()
                    updatePeekHeightTopmost()
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    runPendingSheetAction()
                    updatePeekHeightTopmost()
                }
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    val isOpened: Boolean
        get() = bottomSheet.state != BottomSheetBehavior.STATE_HIDDEN

    var pendingOpen: Int = PENDING_OPEN_NO
        private set
    var openTriggered: Boolean = false
        private set

    init {
        bottomSheet.apply {
            isHideable = true
            isDraggable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            addBottomSheetCallback(bottomSheetCallback)
        }
        recycler.apply {
            adapter = this@MapBottomPanelSheet.adapter
            layoutManager = LinearLayoutManager(sheetView.context)
            overScrollMode = View.OVER_SCROLL_NEVER
            setHasFixedSize(true)
        }
    }

    private fun updatePeekHeightTopmost() {
        updatePeekHeight(adapter.topmostHeight())
    }

    private fun updatePeekHeight(height: Int) {
        if (openTriggered) {
            openTriggered = false
            val pad = topPadViews.fold(0) { acc, view ->
                val params = view.layoutParams as LinearLayout.LayoutParams
                acc + view.height + params.topMargin + params.bottomMargin
            }
            bottomSheet.setPeekHeight(pad + height, true)
        }
    }

    fun addSheetStateCallbackPre(f: (View, Int) -> Unit) {
        sheetStateCallbacksPre.add(f)
    }

    private fun panelHideImpl(forReopen: Int) {
        pendingOpen = forReopen
        bottomSheet.apply {
            if (state != BottomSheetBehavior.STATE_HIDDEN)
                state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    fun panelHide(after: (() -> Unit)? = null) {
        if (!isOpened)
            after?.let { it() }
        else {
            pendingSheetAction = after
            panelHideImpl(PENDING_OPEN_NO)
        }
    }

    fun panelExpandCollapse(collapse: Boolean, after: (() -> Unit)? = null) {
        val newState =
            if (collapse) BottomSheetBehavior.STATE_COLLAPSED
            else BottomSheetBehavior.STATE_EXPANDED
        if (bottomSheet.state == newState)
            after?.let { it() }
        else {
            pendingSheetAction = after
            bottomSheet.state = newState
        }
    }

    fun panelShow(openedBehavior: Int, collapsed: Boolean, prepare: () -> Unit) {
        val newState =
            if (collapsed) BottomSheetBehavior.STATE_COLLAPSED
            else BottomSheetBehavior.STATE_EXPANDED
        if (!isOpened) {
            prepare()
            openTriggered = true
            pendingOpen = PENDING_OPEN_NO
            bottomSheet.state = newState
        } else {
            when (openedBehavior) {
                OPENED_REOPEN -> {
                    val pending =
                        if (collapsed) PENDING_OPEN_COLLAPSE
                        else PENDING_OPEN_EXPAND
                    openTriggered = true
                    pendingSheetAction = prepare
                    panelHideImpl(pending)
                }
                OPENED_CHANGE_VIEW -> {
                    prepare()
                    updatePeekHeightTopmost()
                    bottomSheet.state = newState
                }
                OPENED_IGNORE -> {}
            }
        }
    }
}