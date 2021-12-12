package org.ametro.ui.widgets

import android.animation.Animator
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.util.Pair
import kotlinx.parcelize.Parcelize
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.Constants
import org.ametro.databinding.WidgetMapBottomPanelBinding
import org.ametro.model.MapContainer
import org.ametro.model.ModelUtil
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.model.entities.MapStationInformation

class MapBottomPanelWidget(private val view: ViewGroup,
                           binding: WidgetMapBottomPanelBinding,
                           private val app: ApplicationEx,
                           private val listener: IMapBottomPanelEventListener) :
    Animator.AnimatorListener {

    private val stationTextView = binding.station
    private val lineTextView = binding.line
    private val stationLayout = binding.stationLayout
    private val detailsHint = binding.detailsIcon
    private val detailsProgress = binding.detailsLoading
    private val lineIcon = binding.lineIcon
    private val beginButton = binding.actionStart
    private val endButton = binding.actionEnd

    init {
        val progressTint =
            view.context.resources.getColor(R.color.panel_background_button_icon)
        detailsProgress.indeterminateDrawable.mutate().apply {
            setColorFilter(progressTint, PorterDuff.Mode.SRC_IN)
            detailsProgress.indeterminateDrawable = this
        }
    }

    private val hideAnimation = Runnable {
        app.bottomPanelOpen = false
        app.bottomPanelStation = null
        
        view.animate()
            .setDuration(Constants.ANIMATION_DURATION)
            .setListener(this@MapBottomPanelWidget)
            .translationY(view.height.toFloat())
    }

    private val showAnimation = Runnable {
        view.visibility = View.VISIBLE
        stationTextView.text = station!!.displayName
        lineTextView.text = line!!.displayName
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
        (lineIcon.drawable as GradientDrawable).setColor(line!!.lineColor)

        app.bottomPanelOpen = true
        app.bottomPanelStation = Pair(line, station)

        view.animate()
            .setDuration(Constants.ANIMATION_DURATION)
            .setListener(this@MapBottomPanelWidget)
            .translationY(0f)
    }

    private var actionOnEndAnimation: Runnable? = null
    var isOpened: Boolean = false
        private set

    private val detailsVisibility
        get() = if (hasDetails) View.VISIBLE else View.INVISIBLE

    private var wasOpened = false
    private var firstTime: Boolean = true
    private var hasDetails: Boolean = false
    private var line: MapSchemeLine? = null
    private var station: MapSchemeStation? = null

    init {
        val clickListener = View.OnClickListener { v ->
            if (v === stationLayout && hasDetails) {
                detailsHint.visibility = View.INVISIBLE
                detailsProgress.visibility = View.VISIBLE
                this@MapBottomPanelWidget.listener.onShowMapDetail(line, station)
            } else if (v === beginButton) {
                this@MapBottomPanelWidget.listener.onSelectBeginStation(line, station)
            } else if (v === endButton) {
                this@MapBottomPanelWidget.listener.onSelectEndStation(line, station)
            }
        }
        view.setOnClickListener(clickListener)
        stationLayout.setOnClickListener(clickListener)
        beginButton.setOnClickListener(clickListener)
        endButton.setOnClickListener(clickListener)
    }

    fun detailsClosed() {
        detailsProgress.visibility = View.INVISIBLE
        detailsHint.visibility = detailsVisibility
    }

    fun show(line: MapSchemeLine, station: MapSchemeStation, showDetails: Boolean) {
        if (isOpened && this.line === line && this.station === station) {
            return
        }

        this.line = line
        this.station = station
        this.hasDetails = showDetails
        this.wasOpened = false

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