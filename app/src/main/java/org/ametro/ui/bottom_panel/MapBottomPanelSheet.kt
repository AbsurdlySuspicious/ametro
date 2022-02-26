package org.ametro.ui.bottom_panel

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.app.ApplicationEx
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.utils.misc.AnimUtils
import org.ametro.utils.misc.BottomSheetUtils
import org.ametro.utils.ui.applyInsets
import org.ametro.utils.ui.makeBottomInsetsApplier
import org.ametro.utils.ui.makeTopInsetsApplier
import java.util.concurrent.ConcurrentLinkedQueue

interface BottomPanelSheetListener {
    fun updatePanelPadding(newPadding: Int)
}

class MapBottomPanelSheet(
    val sheetView: NestedScrollView,
    val app: ApplicationEx,
    private val listener: BottomPanelSheetListener,
    private val activity: Activity
) {
    companion object {
        const val PENDING_OPEN_NO = 0
        const val PENDING_OPEN_EXPAND = 1
        const val PENDING_OPEN_COLLAPSE = 2

        const val OPENED_IGNORE = 0
        const val OPENED_CHANGE_VIEW = 1
        const val OPENED_REOPEN = 2
    }

    private val density = activity.resources.displayMetrics.density

    val binding = WidgetMapBottomPanelBinding.bind(sheetView)
    val bottomSheet = BottomSheetBehavior.from(sheetView)
    val adapter = BottomPanelController(binding)

    private val topPadViews = listOf(binding.drag)
    private var bottomInset: Int = 0
    private var isNonTopHidden = false
    private val nonTopInterpolator = DecelerateInterpolator()

    private var sheetStateCallbacksPre: MutableList<(View, Int) -> Unit> = mutableListOf()
    private val pendingSheetActions = ConcurrentLinkedQueue<Pair<Int, () -> Unit>>()

    private fun runPendingSheetActions(state: Int) {
        var action = pendingSheetActions.poll()
        while (action != null) {
            val actState = action.first
            if (state == actState || actState < 0) action.second()
            else pendingSheetActions.offer(action)
            action = pendingSheetActions.poll()
        }
    }

    private fun queueSheetAction(state: Int, action: (() -> Unit)?) {
        action?.let { pendingSheetActions.offer(Pair(state, it)) }
    }

    private fun queueState(f: () -> Unit) {
        if (bottomSheet.state == BottomSheetBehavior.STATE_SETTLING)
            queueSheetAction(-1, f)
        else
            f()
    }

    private fun getSheetHeight(sheetView: View): Int {
        val cfg = activity.resources.configuration
        val win = (cfg.screenHeightDp * density).toInt()
        val loc = intArrayOf(0, 0)
        sheetView.getLocationInWindow(loc)
        return (win - loc[1]).coerceAtLeast(0)
    }

    private fun showHideNonTopViews(show: Boolean) {
        if (show && !isNonTopHidden)
            return
        else if (!show && isNonTopHidden)
            return
        isNonTopHidden = !show

        val views = adapter.nonTopLayouts()
        AnimUtils.getValueAnimator(show, 150L, nonTopInterpolator) { p ->
            views.forEach { it.alpha = p }
        }.also { it.start() }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(sheetView: View, newState: Int) {
            Log.d("AM2", "Bottom sheet state: ${BottomSheetUtils.stateToString(newState)}")

            when (newState) {
                BottomSheetBehavior.STATE_DRAGGING,
                BottomSheetBehavior.STATE_SETTLING -> {}
                BottomSheetBehavior.STATE_HIDDEN ->
                    listener.updatePanelPadding(0)
                else ->
                    listener.updatePanelPadding(getSheetHeight(sheetView))
            }

            sheetStateCallbacksPre
                .forEach { it(sheetView, newState) }

            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    runPendingSheetActions(newState)
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
                    runPendingSheetActions(newState)
                    showHideNonTopViews(false)
                    updatePeekHeightTopmostImpl(true)
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    runPendingSheetActions(newState)
                    showHideNonTopViews(true)
                    updatePeekHeightTopmostImpl(true)
                }
                else -> {
                    showHideNonTopViews(true)
                }
            }
        }

        override fun onSlide(sheetView: View, slideOffset: Float) {
            listener.updatePanelPadding(getSheetHeight(sheetView))
        }
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
        sheetView.also {
            it.clipToPadding = false
            val applier = makeBottomInsetsApplier(it, keepHeight = true)
            applyInsets(applier) { insets ->
                @SuppressLint("NewApi") // won't be called anyway if api is less than supported
                bottomInset = applier.getInset(WindowInsetsCompat.toWindowInsetsCompat(insets))
            }
        }
    }

    fun updatePeekHeightTopmostQueue(animate: Boolean, force: Boolean = false) = queueState {
        updatePeekHeightTopmostImpl(animate, force)
    }

    fun updatePeekHeightQueue(height: Int, animate: Boolean, force: Boolean = false) = queueState {
        updatePeekHeightImpl(height, animate, force)
    }

    fun updatePeekHeightTopmostImpl(animate: Boolean, force: Boolean = false) {
        updatePeekHeightImpl(adapter.topmostHeight(), animate, force)
    }

    fun updatePeekHeightImpl(height: Int, animate: Boolean, force: Boolean = false) {
        if (openTriggered || force) {
            if (!force) openTriggered = false
            val pad = topPadViews.fold(0) { acc, view ->
                val params = view.layoutParams as LinearLayout.LayoutParams
                acc + view.height + params.topMargin + params.bottomMargin
            }
            bottomSheet.setPeekHeight(pad + bottomInset + height, animate)
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

    fun panelHide(after: (() -> Unit)? = null) = queueState {
        if (!isOpened)
            after?.let { it() }
        else {
            queueSheetAction(BottomSheetBehavior.STATE_HIDDEN, after)
            panelHideImpl(PENDING_OPEN_NO)
        }
    }

    fun panelExpandCollapse(collapse: Boolean, after: (() -> Unit)? = null) = queueState {
        val newState =
            if (collapse) BottomSheetBehavior.STATE_COLLAPSED
            else BottomSheetBehavior.STATE_EXPANDED
        if (bottomSheet.state == newState)
            after?.let { it() }
        else {
            queueSheetAction(newState, after)
            bottomSheet.state = newState
        }
    }

    fun panelShow(openedBehavior: Int, collapsed: Boolean, prepare: () -> Unit) = queueState {
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
                    queueSheetAction(BottomSheetBehavior.STATE_HIDDEN, prepare)
                    panelHideImpl(pending)
                }
                OPENED_CHANGE_VIEW -> {
                    prepare()
                    if (bottomSheet.state == newState)
                        updatePeekHeightTopmostImpl(collapsed, true)
                    else
                        updatePeekHeightTopmostImpl(false, true)
                    bottomSheet.state = newState
                }
                OPENED_IGNORE -> {}
            }
        }
    }
}
