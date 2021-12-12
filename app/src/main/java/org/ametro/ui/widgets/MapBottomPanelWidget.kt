package org.ametro.ui.widgets

import android.animation.Animator
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import kotlinx.parcelize.Parcelize
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
                           private val listener: IMapBottomPanelEventListener) :
    Animator.AnimatorListener {

    @Parcelize
    private data class SavedState(
        val schemeName: String,
        val stationUid: Int
    ): Parcelable

    private val instanceStateKey = "bottom-panel-state"

    private val stationTextView = binding.station
    private val lineTextView = binding.line
    private val stationLayout = binding.stationLayout
    private val detailsHint = binding.detailsText
    private val beginButton = binding.buttonBegin
    private val endButton = binding.buttonEnd

    private val hideAnimation = Runnable {
        view.animate()
            .setDuration(Constants.ANIMATION_DURATION)
            .setListener(this@MapBottomPanelWidget)
            .translationY(view.height.toFloat())
    }

    private val showAnimation = Runnable {
        view.visibility = View.VISIBLE
        stationTextView.text = station!!.displayName
        lineTextView.text = line!!.displayName
        detailsHint.visibility = if (hasDetails) View.VISIBLE else View.INVISIBLE
        view.animate()
            .setDuration(Constants.ANIMATION_DURATION)
            .setListener(this@MapBottomPanelWidget)
            .translationY(0f)
    }

    private var actionOnEndAnimation: Runnable? = null
    var isOpened: Boolean = false
        private set

    private var wasOpened = false
    private var firstTime: Boolean = true
    private var hasDetails: Boolean = false
    private var line: MapSchemeLine? = null
    private var station: MapSchemeStation? = null

    init {
        val clickListener = View.OnClickListener { v ->
            if (v === stationLayout && hasDetails) {
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

    fun restoreWindow() {
        Log.i("MEME", "rw open $isOpened, was $wasOpened, line ${line?.name}, station ${station?.name}")
        if (!isOpened && wasOpened && line != null && station != null) {
            Log.i("MEME", "rw fired")
            wasOpened = false
            show(line!!, station!!, hasDetails)
        }
    }

    fun restoreState(bundle: Bundle, container: MapContainer, scheme: MapScheme) {
        Log.i("MEME", "rst begin (scheme ${scheme.name})")
        val state = bundle.getParcelable<SavedState>(instanceStateKey) ?: return
        Log.i("MEME", "rst state $state")
        if (state.schemeName != scheme.name) return
        Log.i("MEME", "rst scheme valid")
        val pack = ModelUtil.findStationByUid(scheme, state.stationUid.toLong()) ?: return
        Log.i("MEME", "rst found station")
        val info: MapStationInformation? = container.findStationInformation(pack.first.name, pack.second.name)
        Log.i("MEME", "rst info ${info?.mapFilePath}")
        this.line = pack.first
        this.station = pack.second
        this.hasDetails = info?.mapFilePath != null
        this.wasOpened = true
        Log.i("MEME", "rst done")
    }

    fun saveState(bundle: Bundle, schemeName: String) {
        Log.i("MEME", "sav begin (scheme $schemeName)")
        Log.i("MEME", "sav op $isOpened, line ${line != null}, station ${station != null}")
        if (!isOpened || line == null || station == null) return
        Log.i("MEME", "sav null checks ok")
        val state = SavedState(schemeName, station!!.uid)
        Log.i("MEME", "sav state created")
        bundle.putParcelable(instanceStateKey, state)
        Log.i("MEME", "sav done")
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