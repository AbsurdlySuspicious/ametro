package org.ametro.utils.misc

import com.google.android.material.bottomsheet.BottomSheetBehavior

object BottomSheetUtils {
    fun stateToString(state: Int): String {
        return when (state) {
            BottomSheetBehavior.STATE_COLLAPSED -> "COLLAPSED"
            BottomSheetBehavior.STATE_DRAGGING -> "DRAGGING"
            BottomSheetBehavior.STATE_EXPANDED -> "EXPANDED"
            BottomSheetBehavior.STATE_HALF_EXPANDED -> "HALF_EXPANDED"
            BottomSheetBehavior.STATE_HIDDEN -> "HIDDEN"
            BottomSheetBehavior.STATE_SETTLING -> "SETTLING"
            else -> "unknown $state"
        }
    }
}
