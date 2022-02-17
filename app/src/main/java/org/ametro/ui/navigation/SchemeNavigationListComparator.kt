package org.ametro.ui.navigation

import org.ametro.model.entities.MapDelayType
import org.ametro.model.entities.MapDelayWeekdayType
import org.ametro.ui.navigation.helpers.DelayResources
import org.ametro.R
import org.ametro.model.entities.MapDelay
import org.ametro.model.entities.MapMetadata.Scheme
import java.util.Comparator

class SchemeNavigationListComparator : Comparator<Scheme> {
    override fun compare(lhs: Scheme, rhs: Scheme): Int {
        val byType = getType(lhs).compareTo(getType(rhs))
        return if (byType != 0) {
            byType
        } else getDisplayName(lhs).compareTo(getDisplayName(rhs))
    }

    private fun getType(scheme: Scheme): String {
        val type = if (scheme.typeName == "ROOT") scheme.displayName else scheme.typeDisplayName
        if (scheme.name == "metro" || scheme.typeName == "Метро") {
            return ""
        }
        return if (type == "OTHER") {
            "" + Character.MAX_VALUE
        } else type
    }

    private fun getDisplayName(scheme: Scheme): String {
        return if (scheme.typeName == "ROOT") "" else scheme.displayName
    }
}