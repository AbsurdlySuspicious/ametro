package org.ametro.providers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import org.ametro.R
import org.ametro.catalog.entities.TransportType
import org.ametro.utils.misc.mapArray

class TransportIconsProvider(context: Context) {
    private val icons: Map<TransportType, Drawable> = createIconsMap(context)

    fun getTransportIcon(transportType: TransportType): Drawable? {
        return icons[transportType]
    }

    fun getTransportIcons(transportTypes: Array<TransportType>): Array<Drawable?> {
        return transportTypes.mapArray { icons[it] }
    }

    companion object {
        private fun createIconsMap(context: Context): HashMap<TransportType, Drawable> {
            val iconsMap = HashMap<TransportType, Drawable>()

            val transportTypes = arrayOf(
                TransportType.Unknown,
                TransportType.Bus,
                TransportType.CableWay,
                TransportType.Subway,
                TransportType.Train,
                TransportType.Tram,
                TransportType.TrolleyBus,
                TransportType.WaterBus
            )

            for (t in transportTypes) {
                ContextCompat
                    .getDrawable(context, getTransportTypeIcon(t))
                    ?.let { iconsMap[t] = it }
            }

            return iconsMap
        }

        private fun getTransportTypeIcon(transportType: TransportType): Int {
            return when (transportType) {
                TransportType.Subway -> R.drawable.icon_b_metro
                TransportType.Tram -> R.drawable.icon_b_tram
                TransportType.Bus -> R.drawable.icon_b_bus
                TransportType.Train -> R.drawable.icon_b_train
                TransportType.WaterBus -> R.drawable.icon_b_water_bus
                TransportType.TrolleyBus -> R.drawable.icon_b_trolleybus
                TransportType.CableWay -> R.drawable.icon_b_cableway
                else -> R.drawable.icon_b_unknown
            }
        }
    }
}