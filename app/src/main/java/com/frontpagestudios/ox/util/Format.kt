package com.frontpagestudios.ox.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Format {
    fun distance(m: Double): String =
        if (m < 1000) "${m.roundToInt()} m" else String.format(Locale.US, "%.1f km", m / 1000)

    /** value + unit, for big-number layouts */
    fun distanceParts(m: Double): Pair<String, String> =
        if (m < 1000) "${m.roundToInt()}" to "m" else String.format(Locale.US, "%.1f", m / 1000) to "km"

    fun duration(ms: Long): String {
        val totalMin = (ms / 60000).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            totalMin > 0 -> "$m min"
            else -> "${(ms / 1000).toInt()} sec"
        }
    }

    fun durationParts(ms: Long): Pair<String, String> {
        val totalMin = (ms / 60000).toInt()
        return if (totalMin >= 60) String.format(Locale.US, "%d:%02d", totalMin / 60, totalMin % 60) to "hr"
        else "$totalMin" to "min"
    }

    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
    }

    fun kmh(mps: Float): Int = (mps * 3.6f).roundToInt()
    fun kmh(mps: Double): Int = (mps * 3.6).roundToInt()
    fun speed(mps: Float): String = "${kmh(mps)} km/h"

    fun time(ts: Long): String = SimpleDateFormat("h:mm a", Locale.US).format(Date(ts))

    fun day(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val today = Calendar.getInstance()
        val yest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        fun same(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return when {
            same(c, today) -> "Today"
            same(c, yest) -> "Yesterday"
            else -> SimpleDateFormat("EEE, d MMM", Locale.US).format(Date(ts))
        }
    }

    fun fullDate(ts: Long): String = SimpleDateFormat("EEEE, d MMMM", Locale.US).format(Date(ts))

    fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good\nmorning,"
        in 12..16 -> "Good\nafternoon,"
        in 17..21 -> "Good\nevening,"
        else -> "Late\nnight,"
    }
}
