package org.ametro.ui.bottom_panel

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.ametro.databinding.WidgetBotRoutePageBinding
import org.ametro.databinding.WidgetItemBotRouteBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.utils.misc.saturate
import java.util.*

class MapBottomPanelRoute(private val sheet: MapBottomPanelSheet, private val listener: MapBottomPanelRouteListener) :
    PanelAdapterBinder<WidgetItemBotRouteBinding> {

    private lateinit var binding: WidgetItemBotRouteBinding
    private var slideHandler: ((Int) -> Unit)? = null
    private var currentPage: Int = 0
    private val adapter = RoutePagerAdapter(sheet.sheetView.context, listener)

    init {
        sheet.addSheetStateCallbackPre { _, state ->
            when (state) {
                BottomSheetBehavior.STATE_HIDDEN ->
                    if (sheet.adapter.showRoute) {
                        listener.onPanelHidden()
                    }
            }
        }
        sheet.adapter.routeBinder = this
    }

    override fun createPanel(bind: WidgetItemBotRouteBinding) {
        binding = bind
        bind.pager.adapter = adapter

        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(bind.pager)
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, (touchSlop * 0.3f).toInt())
        } catch (_: Exception) {
        } // fuck it
    }

    override fun attachItem() {
        binding.pager.setCurrentItem(currentPage, false)
        binding.pager.setPageTransformer(animationPageTransformer)
        binding.pager.registerOnPageChangeCallback(pageChangedCallback)
        binding.pager.registerOnPageChangeCallback(animationPageChangeCallback)
        binding.pager.offscreenPageLimit = 2

        binding.dots.setViewPager2(binding.pager)
        binding.dots.refreshDots()
    }

    override fun detachItem() {
        binding.pager.unregisterOnPageChangeCallback(pageChangedCallback)
        binding.pager.unregisterOnPageChangeCallback(animationPageChangeCallback)
        binding.dots.pager?.removeOnPageChangeListener()
    }

    fun show(routes: ArrayList<RoutePagerItem>, leaveTime: Calendar?, setPage: Int) {
        sheet.panelShow(MapBottomPanelSheet.OPENED_CHANGE_VIEW, false) {
            adapter.leaveTime = leaveTime
            adapter.replaceItems(routes, currentPage, setPage <= 0)
            this.setPage(setPage)
            sheet.adapter.showRoute = true
        }
    }

    fun hide() {
        val after = {
            sheet.adapter.showRoute = false
            adapter.leaveTime = null
        }

        if (!sheet.adapter.showRoute || sheet.adapter.showStation)
            after()
        else
            sheet.panelHide(after)
    }

    fun setSlideCallback(f: (Int) -> Unit) {
        slideHandler = f
    }

    fun setPage(i: Int, smooth: Boolean = false) {
        currentPage = i
        binding?.pager?.setCurrentItem(i, smooth)
    }

    private val pageChangedCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentPage = position
            slideHandler?.let { it(position) }
        }
    }

    // == animations ==

    private val interpolator = DecelerateInterpolator(1.3f)

    private fun halfValue(target: Float, limit: Float): Float {
        val value =
            if (target < 0.5f)
                1f - (1f / 0.5f * target)
            else
                1f / 0.5f * (target - 0.5f)
        return (1f - limit) + limit * value
    }

    @Suppress("UnnecessaryVariable")
    private fun animatePage(bind: WidgetBotRoutePageBinding, offset: Float) {
        bind.transfersRecycler.touchAnimate(offset)

        val timeThis = arrayOf(bind.routeTime, bind.routeTimeSec)
        val timeNext = arrayOf(bind.nextRouteTime, bind.nextRouteTimeSec)

        val nextWidth = timeNext.fold(0) { acc, v ->
            val lp = (v.layoutParams as ConstraintLayout.LayoutParams)
            acc + lp.leftMargin + lp.rightMargin + v.width
        }

        val interpolatedOffset = interpolator.getInterpolation(offset)
        val transX = nextWidth * -offset
        val targetAlpha = saturate(offset, 0.1f, 0f, 0.95f, 1f)
        val halfAlpha = halfValue(targetAlpha, 1f)
        val scaleTxf = halfValue(interpolatedOffset, 0.15f).coerceAtLeast(0.95f)


        timeThis.forEach {
            it.translationX = transX
            it.alpha = 1f - targetAlpha
        }

        timeNext.forEach {
            it.translationX = transX
            it.alpha = targetAlpha
        }

        bind.transfersRecycler.also {
            it.alpha = halfAlpha.coerceAtLeast(0.55f)
            it.scaleX = scaleTxf
            it.scaleY = scaleTxf
        }

        /*arrayOf(bind.routeTimeRangeArrive, bind.routeTimeRangeLeave, bind.routeTimeRangeIcon)
            .forEach { it.alpha = halfAlpha }*/
    }

    private val animationPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
            val holder = adapter.recycler
                ?.findViewHolderForAdapterPosition(position)!! as RoutePagerAdapter.PageHolder
            animatePage(holder.binding, offset)
        }
    }

    private val animationPageTransformer = object : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            if (position < 0) {
                if (position > -1)
                    view.translationX = view.width * -position
                else
                    view.translationX = 0f
            } else {
                if (position > 0)
                    view.translationX = view.width * (1f - position)
                else
                    view.translationX = 0f
            }
        }
    }

    interface MapBottomPanelRouteListener {
        fun onPanelHidden()
        fun onOpenDetails(station: Pair<MapSchemeLine, MapSchemeStation>)
    }
}
