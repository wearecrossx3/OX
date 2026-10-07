package com.frontpagestudios.ox.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.frontpagestudios.ox.data.Trip
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.ui.Routes
import com.frontpagestudios.ox.ui.components.AnimatedNumber
import com.frontpagestudios.ox.ui.components.CircleButton
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.StatTile
import com.frontpagestudios.ox.ui.components.WeekBars
import com.frontpagestudios.ox.ui.components.icon
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Sfx

private val scopes = listOf("Commute", "Walks", "All")

@Composable
private fun StatsTabBody(nav: NavHostController) {
    val trips by TripRepo.trips.collectAsState()
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var scope by rememberSaveable { mutableIntStateOf(0) }
    val filter: (Trip) -> Boolean = when (scope) {
        0 -> { t -> t.mode.isVehicle }
        1 -> { t -> t.mode == TripMode.WALK }
        else -> { _ -> true }
    }
    val w = remember(trips, offset, scope) { weekSummary(trips, offset, filter) }
    val prev = remember(trips, offset, scope) { weekSummary(trips, offset - 1, filter) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (offset >= -1) w.label.replace(" ", "\n") else w.label,
                style = if (offset >= -1) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall,
                color = Ink, modifier = Modifier.weight(1f),
            )
            CircleButton(Icons.AutoMirrored.Rounded.ArrowBack, size = 50.dp) { offset-- }
            Spacer(Modifier.width(8.dp))
            CircleButton(
                Icons.AutoMirrored.Rounded.ArrowForward, size = 50.dp,
                bg = if (offset < 0) Ink else Mist, fg = if (offset < 0) Lime else Muted,
            ) { if (offset < 0) offset++ }
        }
        Spacer(Modifier.height(18.dp))

        // scope switch
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Snow).padding(5.dp),
        ) {
            scopes.forEachIndexed { i, s ->
                val bg by animateColorAsState(if (i == scope) Ink else Snow, tween(220), label = "sc")
                Box(
                    Modifier.weight(1f).pressable { scope = i }.clip(RoundedCornerShape(50)).background(bg)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(s, style = MaterialTheme.typography.labelLarge, color = if (i == scope) Lime else Ink) }
            }
        }
        Spacer(Modifier.height(14.dp))

        // hero
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(40.dp)).background(Ink).padding(24.dp)
        ) {
            SectionLabel(if (scope == 1) "Avg walk time" else "Avg trip time", color = Snow.copy(alpha = 0.5f))
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedNumber(w.avgTime / 60000f, MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f), Lime)
                Spacer(Modifier.width(8.dp))
                Text("min", style = MaterialTheme.typography.headlineMedium, color = Lime.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 14.dp))
                Spacer(Modifier.weight(1f))
                if (prev.count > 0 && w.count > 0) {
                    val diff = ((w.avgTime - prev.avgTime) / 60000).toInt()
                    val better = diff <= 0
                    Box(
                        Modifier.padding(bottom = 14.dp).clip(RoundedCornerShape(50))
                            .background(if (better) Lime else Snow).padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (diff == 0) "same as last wk" else "${if (diff > 0) "+" else "−"}${kotlin.math.abs(diff)} min vs last wk",
                            style = MaterialTheme.typography.labelMedium, color = Ink,
                        )
                    }
                }
            }
            Text(
                "${w.count} trips · ${Format.duration(w.totalTime)} on the move",
                style = MaterialTheme.typography.bodyMedium, color = Snow.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(24.dp))
            WeekBars(w.perDayMin, dayLabels, w.todayIndex, Modifier.fillMaxWidth().height(170.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NavCard("Monthly recap", "Share-ready card", Icons.Rounded.CalendarMonth, Lime, Modifier.weight(1f)) { nav.navigate("recap") }
            NavCard("Signal hotspots", "Where you wait most", Icons.Rounded.Traffic, com.frontpagestudios.ox.ui.theme.Amber, Modifier.weight(1f)) { nav.navigate("hotspots") }
        }
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val (dv, du) = Format.distanceParts(w.totalDist)
            StatTile(dv, du, "Total distance", Modifier.weight(1f))
            StatTile("${Format.kmh(w.avgSpeed)}", "km/h", "Avg speed", Modifier.weight(1f), lime = true, icon = Icons.Rounded.Speed)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${Format.kmh(w.topSpeed)}", "km/h", "Top speed", Modifier.weight(1f), dark = true, icon = Icons.Rounded.Bolt)
            val (tv, tu) = Format.durationParts(w.totalTime)
            StatTile(tv, tu, "Time on the move", Modifier.weight(1f))
        }

        val waitMs = remember(w) { w.trips.filter { it.mode.isVehicle }.sumOf { com.frontpagestudios.ox.util.Stops.totalMs(com.frontpagestudios.ox.util.Stops.find(it.points)) } }
        val fuel = remember(w) { w.trips.mapNotNull { com.frontpagestudios.ox.data.Prefs.fuelCost(it.mode, it.distance) }.sum() }
        if (scope != 1) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val (wv, wu) = Format.durationParts(waitMs)
                StatTile(wv, wu, "Waiting at signals", Modifier.weight(1f))
                StatTile(
                    if (fuel > 0) "₹" + String.format(java.util.Locale.US, "%.0f", fuel) else "—", null,
                    if (fuel > 0) "Fuel spent (est.)" else "Set fuel price in Settings", Modifier.weight(1f), dark = true,
                )
            }
        }

        if (w.trips.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("By mode")
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TripMode.entries.forEach { m ->
                    val list = w.trips.filter { it.mode == m }
                    if (list.isNotEmpty()) ModeBar(m, list.size, list.sumOf { it.durationMs }, w.totalTime)
                }
            }

            val fastest = w.trips.minByOrNull { it.durationMs }
            val slowest = w.trips.maxByOrNull { it.durationMs }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Highlights")
            Spacer(Modifier.height(10.dp))
            if (fastest != null) Highlight("Quickest trip", fastest) { nav.navigate(Routes.trip(fastest.id)) }
            if (slowest != null && slowest.id != fastest?.id) {
                Spacer(Modifier.height(8.dp))
                Highlight("Longest trip", slowest) { nav.navigate(Routes.trip(slowest.id)) }
            }
        }
        Spacer(Modifier.height(150.dp))
    }
}

@Composable
private fun ModeBar(mode: TripMode, count: Int, time: Long, total: Long) {
    val frac = if (total > 0) time.toFloat() / total else 0f
    val anim by animateFloatAsState(frac, tween(900, easing = FastOutSlowInEasing), label = "mb")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Mist), contentAlignment = Alignment.Center) {
            Icon(mode.icon(), null, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(mode.label, style = MaterialTheme.typography.titleSmall, color = Ink, modifier = Modifier.weight(1f))
                Text("$count · ${Format.duration(time)}", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Mist)) {
                Box(Modifier.fillMaxWidth(anim.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(Ink))
            }
        }
    }
}

@Composable
private fun Highlight(label: String, trip: Trip, onClick: () -> Unit) {
    Row(
        Modifier.pressable(sound = Sfx.S.TAP, onClick = onClick).fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Snow).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SectionLabel(label)
            Text(trip.title, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${Format.day(trip.start)} · ${Format.distance(trip.distance)}", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Text(Format.duration(trip.durationMs), style = MaterialTheme.typography.headlineSmall, color = Ink)
    }
}

@Composable
fun StatsTab(nav: NavHostController) {
    val places by com.frontpagestudios.ox.data.Places.places.collectAsState()
    androidx.compose.runtime.key(places) { StatsTabBody(nav) }
}

@Composable
private fun NavCard(
    title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    Column(modifier.pressable(onClick = onClick).clip(RoundedCornerShape(30.dp)).background(Snow).padding(16.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
