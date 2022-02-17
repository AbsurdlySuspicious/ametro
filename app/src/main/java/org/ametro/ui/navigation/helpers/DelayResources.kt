package org.ametro.ui.navigation.helpers

import org.ametro.model.entities.MapDelayType
import org.ametro.model.entities.MapDelayWeekdayType
import org.ametro.ui.navigation.helpers.DelayResources
import org.ametro.R
import org.ametro.model.entities.MapDelay
import org.ametro.model.entities.MapMetadata.Scheme
import java.util.HashMap

object DelayResources {
    private val delayTypeMap = HashMap<MapDelayType, Int>().also {
        it[MapDelayType.NotDefined] = R.string.delay_type_not_defined
        it[MapDelayType.Custom] = R.string.delay_type_custom
        it[MapDelayType.Day] = R.string.delay_type_day
        it[MapDelayType.Night] = R.string.delay_type_night
        it[MapDelayType.Evening] = R.string.delay_type_evening
        it[MapDelayType.Mourning] = R.string.delay_type_morning
        it[MapDelayType.Rush] = R.string.delay_type_rush
        it[MapDelayType.Direct] = R.string.delay_type_direct
        it[MapDelayType.WestNorth] = R.string.delay_type_west_north
        it[MapDelayType.WestSouth] = R.string.delay_type_west_south
        it[MapDelayType.WestEast] = R.string.delay_type_west_east
        it[MapDelayType.EastNorth] = R.string.delay_type_east_north
        it[MapDelayType.EastSouth] = R.string.delay_type_east_south
        it[MapDelayType.EastWest] = R.string.delay_type_east_west
        it[MapDelayType.NorthEast] = R.string.delay_type_north_east
        it[MapDelayType.NorthWest] = R.string.delay_type_north_west
        it[MapDelayType.NorthSouth] = R.string.delay_type_north_south
        it[MapDelayType.SouthEast] = R.string.delay_type_south_east
        it[MapDelayType.SouthWest] = R.string.delay_type_south_west
        it[MapDelayType.SouthNorth] = R.string.delay_type_south_north
    }

    private val delayWeekDaysMap = HashMap<MapDelayWeekdayType, Int>().also {
        it[MapDelayWeekdayType.NotDefined] = R.string.delay_weekdays_type_not_defined
        it[MapDelayWeekdayType.Monday] = R.string.delay_weekdays_type_monday
        it[MapDelayWeekdayType.Tuesday] = R.string.delay_weekdays_type_tuesday
        it[MapDelayWeekdayType.Wednesday] = R.string.delay_weekdays_type_wednesday
        it[MapDelayWeekdayType.Thursday] = R.string.delay_weekdays_type_thursday
        it[MapDelayWeekdayType.Friday] = R.string.delay_weekdays_type_friday
        it[MapDelayWeekdayType.Saturday] = R.string.delay_weekdays_type_saturday
        it[MapDelayWeekdayType.Sunday] = R.string.delay_weekdays_type_sunday
        it[MapDelayWeekdayType.Workdays] = R.string.delay_weekdays_type_workdays
        it[MapDelayWeekdayType.Weekend] = R.string.delay_weekdays_type_weekend
    }

    fun getDelayTypeTextId(type: MapDelayType): Int {
        return delayTypeMap[type]!!
    }

    fun getDelayWeekendTypeTextId(weekdays: MapDelayWeekdayType): Int {
        return delayWeekDaysMap[weekdays]!!
    }
}