package org.ametro.ui.widgets

import android.graphics.Matrix
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import org.ametro.model.entities.MapPoint
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeStation
import org.ametro.ui.views.MultiTouchMapView.IViewportChangedListener
import org.ametro.utils.misc.mapValue
import kotlin.math.roundToInt

class MapSelectionIndicatorsWidget(
    private val listener: IMapSelectionEventListener,
    private val beginIndicator: ImageView,
    private val endIndicator: ImageView
) : IViewportChangedListener {

    private val indicators = arrayOf(beginIndicator, endIndicator)
    private val viewMatrix = Matrix()
    private val viewMatrixValues = FloatArray(9)

    private var beginStation: Pair<MapSchemeLine, MapSchemeStation>? = null
    private var endStation: Pair<MapSchemeLine, MapSchemeStation>? = null

    private fun updateOpacity(o: Float) {
        indicators.forEach { it.alpha = o }
    }

    fun getBeginStation(): Pair<MapSchemeLine, MapSchemeStation>? {
        return beginStation
    }

    fun setBeginStation(station: Pair<MapSchemeLine, MapSchemeStation>?) {
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

    fun getEndStation(): Pair<MapSchemeLine, MapSchemeStation>? {
        return endStation
    }

    fun setEndStation(station: Pair<MapSchemeLine, MapSchemeStation>?) {
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
        matrix.getValues(viewMatrixValues)
        val scale = viewMatrixValues[Matrix.MSCALE_X]
        updateOpacity(mapValue(scale, 0.95f, 1.20f, 0.3f, 1f))
        updateIndicatorsPositionAndState()
    }

    override fun onViewportInitialized() {}

    private fun updateIndicatorsPositionAndState() {
        if (beginStation != null) {
            beginIndicator.visibility = View.VISIBLE
            setViewPosition(beginIndicator, beginStation!!.second.position)
        } else {
            beginIndicator.visibility = View.INVISIBLE
        }
        if (endStation != null) {
            endIndicator.visibility = View.VISIBLE
            setViewPosition(endIndicator, endStation!!.second.position)
        } else {
            endIndicator.visibility = View.INVISIBLE
        }
    }

    private fun setViewPosition(view: View, point: MapPoint) {
        val pts = floatArrayOf(point.x, point.y)
        viewMatrix.mapPoints(pts)
        val p = view.layoutParams as MarginLayoutParams
        p.setMargins((pts[0] - view.width / 2).roundToInt(), (pts[1] - view.height).roundToInt(), 0, 0)
        view.requestLayout()
    }

    interface IMapSelectionEventListener {
        fun onRouteSelectionComplete(begin: Pair<MapSchemeLine, MapSchemeStation>, end: Pair<MapSchemeLine, MapSchemeStation>)
        fun onRouteSelectionCleared()
    }
}