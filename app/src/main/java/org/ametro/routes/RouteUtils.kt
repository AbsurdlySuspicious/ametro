package org.ametro.routes

import androidx.core.util.Pair
import org.ametro.routes.entities.MapRoute
import org.ametro.model.entities.MapScheme
import org.ametro.routes.entities.MapRoutePart
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeSegment
import org.ametro.model.entities.MapSchemeTransfer
import java.util.HashSet

object RouteUtils {
    fun convertRouteToSchemeObjectIds(route: MapRoute, scheme: MapScheme): HashSet<Int> {
        val ids = HashSet<Int>()
        val transfers = HashSet<Pair<Int, Int>>()
        for (part in route.parts) {
            ids.add(part.from)
            ids.add(part.to)
            transfers.add(Pair(part.from, part.to))
        }
        for (line in scheme.lines) {
            for (segment in line.segments) {
                val id = Pair(segment.from, segment.to)
                val reverseId = Pair(segment.to, segment.from)
                if (transfers.contains(id) || transfers.contains(reverseId)) {
                    ids.add(segment.uid)
                }
            }
        }
        for (transfer in scheme.transfers) {
            val id = Pair(transfer.from, transfer.to)
            val reverseId = Pair(transfer.to, transfer.from)
            if (transfers.contains(id) || transfers.contains(reverseId)) {
                ids.add(transfer.uid)
            }
        }
        return ids
    }
}