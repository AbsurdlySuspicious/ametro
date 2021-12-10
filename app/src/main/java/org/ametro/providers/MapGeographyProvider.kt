package org.ametro.providers

import org.ametro.catalog.entities.MapInfo
import org.ametro.utils.misc.*
import java.util.*

open class MapGeographyProvider(maps: Array<MapInfo>) {
    lateinit var countries: Array<String>
        private set
    private lateinit var cities: Array<Array<String>>

    private val countryIsoCodes = HashMap<String, String>()
    private val countryIdentifierMap = HashMap<String, Int>()
    private val cityIdentifierMap = HashMap<String, Int>()

    init {
        setData(maps)
    }

    fun setData(maps: Array<MapInfo>) {
        bindStaticData(maps)
        bindData(maps)
    }

    protected fun bindData(maps: Array<MapInfo>) {
        val localCountries: MutableMap<String, MutableSet<String>> = HashMap()

        for (m in maps) {
            val country = m.country
            val localCities =
                localCountries[country] ?: HashSet<String>().also { localCountries[country] = it }
            localCities.add(m.city)
        }

        countries = localCountries.keys
            .toTypedArray()
            .also { Arrays.sort(it) }
        cities = countries.mapArray { country ->
            localCountries[country]!!
                .toTypedArray()
                .also { Arrays.sort(it) }
        }
    }

    private fun bindStaticData(maps: Array<MapInfo>) {
        var countryId = 1
        var cityId = 1
        countryIsoCodes.clear()
        countryIdentifierMap.clear()
        cityIdentifierMap.clear()
        for (m in maps) {
            val country = m.country
            if (countryIdentifierMap[country] == null) {
                countryIsoCodes[country] = m.iso
                countryIdentifierMap[country] = countryId++
            }
            val city = m.city
            if (cityIdentifierMap[city] == null) {
                cityIdentifierMap[city] = cityId++
            }
        }
    }

    fun getCountryCities(countryIndex: Int): Array<String> {
        return cities[countryIndex]
    }

    fun getCountryIsoCode(country: String): String? {
        return countryIsoCodes[country]
    }

    fun getCountryId(countryName: String): Int {
        return countryIdentifierMap[countryName]!!
    }

    fun getCityId(cityName: String): Int {
        return cityIdentifierMap[cityName]!!
    }
}