package org.ametro.routes

import android.util.Log
import org.ametro.routes.entities.MapRouteQueryParameters
import org.ametro.routes.entities.MapRoute
import org.ametro.routes.algorithms.DijkstraHeap.TransportGraph
import org.ametro.routes.algorithms.DijkstraHeap
import org.ametro.routes.entities.MapRoutePart
import org.ametro.model.entities.MapTransportLine
import java.util.*

object MapRouteProvider {
    fun findRoutes(parameters: MapRouteQueryParameters, maxRoutes: Int): ArrayList<MapRoute> {
        val results = arrayListOf<MapRoute>()
        var lastResult: DijkstraHeap.Result? = null
        val graph = TransportGraph(parameters.stationCount)
        createGraphEdges(graph, parameters)

        var routeI = 0
        while (routeI < maxRoutes) {
            routeI++
            val r = DijkstraHeap.dijkstra(graph, parameters.beginStationUid)

            if (r.predecessors[parameters.endStationUid] == DijkstraHeap.NO_WAY)
                break

            if (r.same(lastResult))
                continue
            lastResult = r

            val mr = convertToMapRouteAndMarkUsed(r, parameters.endStationUid, graph)

            if (mr == null) {
                routeI--
                continue
            }

            results.add(mr)
        }

        return results
    }

    private fun convertToMapRouteAndMarkUsed(
        result: DijkstraHeap.Result,
        endStationUid: Int,
        graph: TransportGraph
    ): MapRoute? {
        Log.i("MEME", "-- mark used pass --")
        val distances = result.distances
        val predecessors = result.predecessors
        val parts = ArrayList<MapRoutePart>()
        var to = endStationUid
        var from = predecessors[to]
        var prevEdge: DijkstraHeap.Edge? = null

        while (from != DijkstraHeap.NO_WAY) {
            val edge = graph.edges[from]
                .firstOrNull { it.end == to }
                ?.also {
                    if (!it.used && it.transfer) {
                        Log.i("MEME", "edge to ${it.start} -> ${it.end}")
                        it.used = true
                    }
                }

            val isTransfer = edge?.transfer ?: false

            if (isTransfer && prevEdge?.transfer == true)
                return null
            prevEdge = edge

            parts.add(MapRoutePart(from, to, distances[to] - distances[from], isTransfer))
            to = from
            from = predecessors[to]
        }

        parts.reverse()
        return MapRoute(parts.toTypedArray())
    }

    private fun createGraphEdges(graph: TransportGraph, parameters: MapRouteQueryParameters) {
        val stationLines = arrayOfNulls<MapTransportLine>(parameters.stationCount)

        for (scheme in parameters.enabledTransportsSchemes) {
            for (line in scheme.lines) {
                for (segment in line.segments) {
                    if (segment.delay == 0)
                        continue
                    graph.addEdge(segment.from, segment.to, segment.delay, false)
                    stationLines[segment.from] = line
                    stationLines[segment.to] = line
                }
            }

            val delayIndex = parameters.delayIndex
            for (transfer in scheme.transfers) {
                var delay = transfer.delay
                if (delayIndex != null && stationLines[transfer.to] != null) {
                    val lineDelays = stationLines[transfer.to]!!.delays
                    if (lineDelays.isNotEmpty())
                        delay += stationLines[transfer.to]!!.delays[delayIndex]
                }
                graph.addEdge(transfer.from, transfer.to, delay, true)
                graph.addEdge(transfer.to, transfer.from, delay, true)
            }
        }
    }
}