package org.ametro.catalog.serialization

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import org.ametro.catalog.entities.*
import org.ametro.utils.misc.*
import org.json.JSONArray
import org.json.JSONObject

object MapCatalogSerializer {
    private val reader: ObjectReader = ObjectMapper().reader()

    @JvmStatic
    @Throws(SerializationException::class)
    fun deserializeMapInfoArray(jsonText: String?): Array<MapInfoEntity> {
        return try {
            val json = reader.readTree(jsonText) ?: return emptyArray()
            json.mapToArraySizedExact(json.size()) { jsonMap ->
                MapInfoEntity(
                    jsonMap["city_id"].asInt(),
                    jsonMap["file"].asText(),
                    jsonMap["latitude"].asDouble(),
                    jsonMap["longitude"].asDouble(),
                    jsonMap["size"].asInt(),
                    jsonMap["timestamp"].asInt(),
                    deserializeTransports(jsonMap["transports"]),
                    jsonMap["uid"].asText()
                )
            }
        } catch (ex: Exception) {
            throw SerializationException(ex)
        }
    }

    @JvmStatic
    @Throws(SerializationException::class)
    fun deserializeLocalization(jsonText: String?): Array<MapInfoEntityName> {
        return try {
            val json = reader.readTree(jsonText) ?: return emptyArray()
            json.mapToArraySizedExact(json.size()) { city ->
                MapInfoEntityName(
                    city[0].asInt(),
                    city[1].asText(),
                    city[2].asText(),
                    city[3].asText()
                )
            }
        } catch (ex: Exception) {
            throw SerializationException(ex)
        }
    }

    @JvmStatic
    @Throws(SerializationException::class)
    fun serializeMapInfoArray(maps: Array<MapInfo>): String {
        return try {
            val jsonMaps = JSONArray()
            for (map in maps) {
                val jsonMap = JSONObject()
                jsonMap.put("city_id", map.cityId)
                jsonMap.put("file", map.fileName)
                jsonMap.put("latitude", map.latitude)
                jsonMap.put("longitude", map.longitude)
                jsonMap.put("size", map.size)
                jsonMap.put("timestamp", map.timestamp)
                jsonMap.put("transports", serializeTransports(map.types))
                jsonMap.put("uid", map.uid)
                jsonMaps.put(jsonMap)
            }
            jsonMaps.toString()
        } catch (ex: Exception) {
            throw SerializationException(ex)
        }
    }

    private fun deserializeTransports(transports: JsonNode): Array<TransportType?> {
        return transports.mapToArraySizedExact(transports.size()) {
            TransportTypeHelper.parseTransportType(it.asText())
        }
    }

    private fun serializeTransports(transportTypes: Array<TransportType>): JSONArray {
        val types = JSONArray()
        for (transportType in transportTypes) {
            types.put(TransportTypeHelper.formatTransportTypeName(transportType))
        }
        return types
    }
}