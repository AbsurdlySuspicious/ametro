package org.ametro.routes

import org.ametro.routes.entities.MapRouteQueryParameters
import org.ametro.routes.entities.MapRoute
import org.ametro.routes.algorithms.DijkstraHeap.TransportGraph
import org.ametro.routes.algorithms.DijkstraHeap
import org.ametro.routes.entities.MapRoutePart
import org.ametro.model.entities.MapTransportLine
import java.util.*

object MapRouteProvider {
    fun findRoutes(parameters: MapRouteQueryParameters): Array<MapRoute> {
        val graph = TransportGraph(parameters.stationCount)

        createGraphEdges(graph, parameters)

        val result = DijkstraHeap.dijkstra(graph, parameters.beginStationUid)

        return when (result.predecessors[parameters.endStationUid]) {
            DijkstraHeap.NO_WAY -> emptyArray()
            else -> arrayOf(convertToMapRoute(result, parameters.endStationUid))
        }
    }

    private fun convertToMapRoute(result: DijkstraHeap.Result, endStationUid: Int): MapRoute {
        val distances = result.distances
        val predecessors = result.predecessors
        val parts = ArrayList<MapRoutePart>()
        var to = endStationUid
        var from = predecessors[to]

        while (from != DijkstraHeap.NO_WAY) {
            parts.add(MapRoutePart(from, to, distances[to] - distances[from]))
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
                    if (segment.delay == 0) {
                        continue
                    }
                    graph.addEdge(segment.from, segment.to, segment.delay)
                    stationLines[segment.from] = line
                    stationLines[segment.to] = line
                }
            }

            val delayIndex = parameters.delayIndex
            for (transfer in scheme.transfers) {
                var delay = transfer.delay
                if (delayIndex != null && stationLines[transfer.to] != null) {
                    val lineDelays = stationLines[transfer.to]!!.delays
                    if (lineDelays.isNotEmpty()) {
                        delay += stationLines[transfer.to]!!.delays[delayIndex]
                    }
                }
                graph.addEdge(transfer.from, transfer.to, delay)
                graph.addEdge(transfer.to, transfer.from, delay)
            }
        }
    }
}