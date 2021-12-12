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

class StationMapFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_station_map_view, container, false)
        val arguments = requireArguments()
        setupWebView(
            rootView.findViewById(R.id.web),
            arguments.getString(Constants.LINE_NAME),
            arguments.getString(Constants.STATION_NAME)
        )
        return rootView
    }

    private fun setupWebView(webView: WebView, lineName: String?, stationName: String?) {
        try {
            val container = ApplicationEx.getInstanceActivity(requireActivity()).container!!
            val station = container.findStationInformation(lineName, stationName)
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
            }
        } catch (e: Exception) {
            webView.visibility = View.GONE
        }
    }
}