package org.ametro.utils

import java.text.Collator

object StringUtils {
    private val COLLATOR = Collator.getInstance()
    private val SI_UNITS = arrayOf("k", "M", "G", "T", "P", "E")
    private val COMPUTING_UNITS = arrayOf("Ki", "Mi", "Gi", "Ti", "Pi", "Ei")

    init {
        COLLATOR.strength = Collator.PRIMARY
    }

    fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) SI_UNITS else COMPUTING_UNITS)[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun startsWithoutDiacritics(text: String, prefix: String): Boolean {
        val textLength = text.length
        val prefixLength = prefix.length
        if (textLength < prefixLength) {
            return false
        }
        val textPrefix = text.substring(0, prefixLength)
        return COLLATOR.compare(textPrefix, prefix) == 0
    }

    @JvmStatic
    fun isNullOrEmpty(value: String?): Boolean {
        return value == null || "" == value.trim { it <= ' ' }
    }

    fun humanReadableTime(totalSeconds: Int): String {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 60 / 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else String.format("%02d:%02d", minutes, seconds)
    }

    fun humanReadableTimeRoute(totalSeconds: Int): Pair<String, String> {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 60 / 60
        val timeString = "%01d:$02d".format(hours, minutes)
        val secondsString = ":%02d".format(seconds)
        return Pair(timeString, secondsString)
    }
}