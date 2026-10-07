package com.frontpagestudios.ox.util

import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Export {
    private fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    fun csv(trips: List<Trip>): String {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val t = SimpleDateFormat("HH:mm", Locale.US)
        val sb = StringBuilder("date,start,end,from,to,mode,auto,km,minutes,avg_kmh,top_kmh,stops,waiting_min,fuel_rs\n")
        trips.sortedBy { it.start }.forEach { tr ->
            val stops = if (tr.mode.isVehicle) Stops.find(tr.points) else emptyList()
            val fuel = Prefs.fuelCost(tr.mode, tr.distance)
            sb.append(d.format(Date(tr.start))).append(',')
                .append(t.format(Date(tr.start))).append(',')
                .append(t.format(Date(tr.end))).append(',')
                .append(q(tr.fromName)).append(',')
                .append(q(tr.toName)).append(',')
                .append(tr.mode.label).append(',')
                .append(if (tr.auto) "yes" else "no").append(',')
                .append(String.format(Locale.US, "%.2f", tr.distance / 1000)).append(',')
                .append(String.format(Locale.US, "%.1f", tr.durationMs / 60000.0)).append(',')
                .append(Format.kmh(tr.avgSpeed)).append(',')
                .append(Format.kmh(tr.maxSpeed)).append(',')
                .append(stops.size).append(',')
                .append(String.format(Locale.US, "%.1f", Stops.totalMs(stops) / 60000.0)).append(',')
                .append(fuel?.let { String.format(Locale.US, "%.0f", it) } ?: "")
                .append('\n')
        }
        return sb.toString()
    }
}
