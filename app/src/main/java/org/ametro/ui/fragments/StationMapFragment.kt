package org.ametro.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import org.ametro.R
import org.ametro.app.ApplicationEx
import org.ametro.app.Constants
import org.ametro.model.entities.MapStationInformation
import org.ametro.utils.ui.*

class StationMapFragment(private val station: MapStationInformation) : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_station_map_view, container, false)
        setupWebView(rootView.findViewById(R.id.web))
        return rootView
    }

    private fun setupWebView(webView: WebView) {
        try {
            val container = ApplicationEx.getInstanceActivity(requireActivity()).container!!
            val mapInSvgFormat = container.loadStationMap(station.mapFilePath)
            webView.apply {
                setInitialScale(1)
                settings.apply {
                    setSupportZoom(true)
                    displayZoomControls = false
                    builtInZoomControls = true
                    useWideViewPort = true
                }
                loadDataWithBaseURL(
                    "not_defined",
                    mapInSvgFormat,
                    "image/svg+xml",
                    "utf-8",
                    "not_defined"
                )
                visibility = View.VISIBLE
                clipToPadding = false
            }
            applyInsets(makeBottomInsetsApplier(webView, keepHeight = true))
        } catch (e: Exception) {
            webView.visibility = View.GONE
        }
    }
}