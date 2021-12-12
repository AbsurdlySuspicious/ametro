package org.ametro.ui.widgets

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.ametro.R
import org.ametro.app.Constants
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation

class MapBottomPanelWidget(private val view: ViewGroup,
                           binding: WidgetMapBottomPanelBinding,
                           private val listener: IMapBottomPanelEventListener) :
    Animator.AnimatorListener {

    private val stationTextView = binding.station
    private val lineTextView = binding.line
    private val detailButton = binding.stationLayout
    private val beginButton = binding.buttonBegin
    private val endButton = binding.buttonEnd

    private val hideAnimation = Runnable {
        view.animate().setDuration(Constants.ANIMATION_DURATION).setListener(this@MapBottomPanelWidget)
            .translationY(view.height.toFloat())
    }

    private val showAnimation = Runnable {
        view.visibility = View.VISIBLE
        stationTextView.text = station!!.displayName
        lineTextView.text = line!!.displayName
        view.animate().setDuration(Constants.ANIMATION_DURATION).setListener(this@MapBottomPanelWidget).translationY(0f)
    }

    private var actionOnEndAnimation: Runnable? = null
    var isOpened: Boolean = false
        private set

    private var firstTime: Boolean = true
    private var line: MapSchemeLine? = null
    private var station: MapSchemeStation? = null

    init {
        val clickListener = View.OnClickListener { v ->
            if (v === detailButton) {
                this@MapBottomPanelWidget.listener.onShowMapDetail(line, station)
            } else if (v === beginButton) {
                this@MapBottomPanelWidget.listener.onSelectBeginStation(line, station)
            } else if (v === endButton) {
                this@MapBottomPanelWidget.listener.onSelectEndStation(line, station)
            }
        }
        view.setOnClickListener(clickListener)
        detailButton.setOnClickListener(clickListener)
        beginButton.setOnClickListener(clickListener)
        endButton.setOnClickListener(clickListener)
    }

    fun show(line: MapSchemeLine, station: MapSchemeStation, showDetails: Boolean) {
        detailButton.visibility = if (showDetails) View.VISIBLE else View.INVISIBLE
        if (isOpened && this.line === line && this.station === station) {
            return
        }
        this.line = line
        this.station = station
        if (!isOpened && !firstTime) {
            isOpened = true
            showAnimation.run()
            return
        }
        isOpened = true
        firstTime = false
        actionOnEndAnimation = showAnimation
        hideAnimation.run()
    }

    fun hide() {
        if (!isOpened) {
            return
        }
        isOpened = false
        hideAnimation.run()
    }

    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationEnd(animation: Animator) {
        if (!isOpened) view.visibility = View.INVISIBLE
        if (actionOnEndAnimation != null) {
            actionOnEndAnimation!!.run()
            actionOnEndAnimation = null
        }
    }

    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
    interface IMapBottomPanelEventListener {
        fun onShowMapDetail(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectBeginStation(line: MapSchemeLine?, station: MapSchemeStation?)
        fun onSelectEndStation(line: MapSchemeLine?, station: MapSchemeStation?)
    }
}