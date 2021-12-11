package org.ametro.ui.widgets

import android.graphics.Matrix
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapSchemeStation
import org.ametro.ui.views.MultiTouchMapView.IViewportChangedListener
import kotlin.math.roundToInt

class MapSelectionIndicatorsWidget(
    private val listener: IMapSelectionEventListener,
    private val beginIndicator: View,
    private val endIndicator: View
) : IViewportChangedListener {

    private val viewMatrix = Matrix()

    private var beginStation: MapSchemeStation? = null
    private var endStation: MapSchemeStation? = null

    fun getBeginStation(): MapSchemeStation? {
        return beginStation
    }

    fun setBeginStation(station: MapSchemeStation?) {
        if (endStation === station) {
            if (beginStation != null && endStation != null) {
                listener.onRouteSelectionCleared()
            }
            endStation = null
        }
        beginStation = station
        updateIndicatorsPositionAndState()
        if (beginStation != null && endStation != null) {
            listener.onRouteSelectionComplete(beginStation!!, endStation!!)
        }
    }

    fun getEndStation(): MapSchemeStation? {
        return endStation
    }

    fun setEndStation(station: MapSchemeStation?) {
        if (beginStation === station) {
            if (beginStation != null && endStation != null) {
                listener.onRouteSelectionCleared()
            }
            beginStation = null
        }
        endStation = station
        updateIndicatorsPositionAndState()
        if (beginStation != null && endStation != null) {
            listener.onRouteSelectionComplete(beginStation!!, endStation!!)
        }
    }

    fun clearSelection() {
        beginStation = null
        endStation = null
        updateIndicatorsPositionAndState()
        listener.onRouteSelectionCleared()
    }

    fun hasSelection(): Boolean {
        return beginStation != null || endStation != null
    }

    override fun onViewportChanged(matrix: Matrix) {
        viewMatrix.set(matrix)
        updateIndicatorsPositionAndState()
    }

    private fun updateIndicatorsPositionAndState() {
        if (beginStation != null) {
            beginIndicator.visibility = View.VISIBLE
            setViewPosition(beginIndicator, beginStation!!.position)
        } else {
            beginIndicator.visibility = View.INVISIBLE
        }
        if (endStation != null) {
            endIndicator.visibility = View.VISIBLE
            setViewPosition(endIndicator, endStation!!.position)
        } else {
            endIndicator.visibility = View.INVISIBLE
        }
    }

    private fun setViewPosition(view: View, point: MapPoint) {
        val pts = FloatArray(2)
        pts[0] = point.x
        pts[1] = point.y
        viewMatrix.mapPoints(pts)
        val p = view.layoutParams as MarginLayoutParams
        p.setMargins((pts[0] - view.width / 4).roundToInt(), (pts[1] - view.height).roundToInt(), 0, 0)
        view.requestLayout()
    }

    interface IMapSelectionEventListener {
        fun onRouteSelectionComplete(begin: MapSchemeStation, end: MapSchemeStation)
        fun onRouteSelectionCleared()
    }
}