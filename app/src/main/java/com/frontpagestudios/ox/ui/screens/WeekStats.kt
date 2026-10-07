package com.frontpagestudios.ox.ui.screens

import com.frontpagestudios.ox.data.Trip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeekSummary(
    val trips: List<Trip>,
    val totalDist: Double,
    val totalTime: Long,
    val avgTime: Long,
    val avgSpeed: Double,
    val topSpeed: Float,
    val perDayMin: List<Float>,
    val todayIndex: Int,
    val label: String,
) {
    val count get() = trips.size
}

val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

private fun weekStart(offset: Int): Calendar = Calendar.getInstance().apply {
    firstDayOfWeek = Calendar.MONDAY
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    val dow = get(Calendar.DAY_OF_WEEK)
    val back = (dow - Calendar.MONDAY + 7) % 7
    add(Calendar.DAY_OF_YEAR, -back)
    add(Calendar.WEEK_OF_YEAR, offset)
}

fun weekSummary(all: List<Trip>, offset: Int = 0, filter: (Trip) -> Boolean = { true }): WeekSummary {
    val start = weekStart(offset)
    val s = start.timeInMillis
    val e = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
    val trips = all.filter { it.start in s until e && filter(it) }
    val perDay = MutableList(7) { 0f }
    trips.forEach { t ->
        val idx = ((t.start - s) / 86_400_000L).toInt().coerceIn(0, 6)
        perDay[idx] += t.durationMs / 60000f
    }
    val totalTime = trips.sumOf { it.durationMs }
    val totalDist = trips.sumOf { it.distance }
    val now = System.currentTimeMillis()
    val today = if (now in s until e) ((now - s) / 86_400_000L).toInt() else -1
    val fmt = SimpleDateFormat("d MMM", Locale.US)
    val label = when (offset) {
        0 -> "This week"
        -1 -> "Last week"
        else -> "${fmt.format(Date(s))} – ${fmt.format(Date(e - 1))}"
    }
    return WeekSummary(
        trips = trips,
        totalDist = totalDist,
        totalTime = totalTime,
        avgTime = if (trips.isNotEmpty()) totalTime / trips.size else 0L,
        avgSpeed = if (totalTime > 0) totalDist / (totalTime / 1000.0) else 0.0,
        topSpeed = trips.maxOfOrNull { it.maxSpeed } ?: 0f,
        perDayMin = perDay,
        todayIndex = today,
        label = label,
    )
}
