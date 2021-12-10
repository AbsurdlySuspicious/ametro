package org.ametro.providers

import org.ametro.catalog.entities.MapInfo
import org.ametro.utils.StringUtils

class FilteringMapGeographyProvider(private val originalValues: Array<MapInfo>) : MapGeographyProvider(
    originalValues
) {
    fun setFilter(criteria: String?) {
        bindData(filterMapsByCountryOrCityName(criteria))
    }

    private fun filterMapsByCountryOrCityName(criteria: String?): Array<MapInfo> {
        if (criteria == null) return originalValues
        val filteredMaps = ArrayList<MapInfo>(originalValues.size)
        for (map in originalValues) {
            if (StringUtils.startsWithoutDiacritics(map.city, criteria)
                || StringUtils.startsWithoutDiacritics(map.country, criteria)
            ) {
                filteredMaps.add(map)
            }
        }
        return filteredMaps.toTypedArray()
    }
}