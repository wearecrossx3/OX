package com.frontpagestudios.ox.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frontpagestudios.ox.data.Trip
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.data.Places
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.util.Stops
import com.frontpagestudios.ox.ui.theme.Amber
import androidx.compose.material.icons.rounded.LocalGasStation
import com.frontpagestudios.ox.ui.components.EndActionsDialog
import com.frontpagestudios.ox.ui.components.RenameTripDialog
import com.frontpagestudios.ox.ui.components.SavePlaceDialog
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.frontpagestudios.ox.ui.components.Chip
import com.frontpagestudios.ox.ui.components.CircleButton
import com.frontpagestudios.ox.ui.components.OxMap
import com.frontpagestudios.ox.ui.components.Reveal
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.SpeedGraph
import com.frontpagestudios.ox.ui.components.StatTile
import com.frontpagestudios.ox.ui.components.icon
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Danger
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Paper
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Geo
import com.frontpagestudios.ox.util.Sfx
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

@Composable
private fun TripDetailScreenBody(id: String, onBack: () -> Unit) {
    val trips by TripRepo.trips.collectAsState()
    val loaded by TripRepo.loaded.collectAsState()
    val trip = trips.firstOrNull { it.id == id }

    if (trip == null) {
        Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
            if (loaded) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Trip not found", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Spacer(Modifier.height(12.dp))
                    Chip("Go back", bg = Lime, modifier = Modifier.pressable { onBack() })
                }
            }
        }
        return
    }
    TripDetail(trip, trips, onBack)
}

@Composable
private fun TripDetail(trip: Trip, all: List<Trip>, onBack: () -> Unit) {
    val geo = remember(trip.id) { Geo.geo(trip.points) }
    val draw = remember(trip.id) { Animatable(0f) }
    LaunchedEffect(trip.id) {
        delay(350)
        Sfx.play(Sfx.S.SWOOSH)
        draw.animateTo(1f, tween(1700, easing = FastOutSlowInEasing))
    }
    val shown = geo.take(max(2, (geo.size * draw.value).toInt()).coerceAtMost(geo.size))
    var confirmDelete by remember { mutableStateOf(false) }
    val stops = remember(trip.id) { if (trip.mode.isVehicle) Stops.find(trip.points) else emptyList() }
    val stopGeo = remember(trip.id) { stops.map { org.osmdroid.util.GeoPoint(it.lat, it.lon) } }
    var endAction by remember { mutableStateOf<Int?>(null) }
    var savePlaceFor by remember { mutableStateOf<Int?>(null) }
    var renaming by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sameMode = remember(all, trip.id) { weekSummary(all, 0) { it.mode == trip.mode && it.id != trip.id } }

    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(440.dp).background(Ink)) {
            OxMap(
                points = shown,
                modifier = Modifier.fillMaxSize(),
                fitKey = trip.id,
                fitPoints = geo,
                lineWidthDp = 5f,
                stops = if (draw.value >= 0.99f) stopGeo else emptyList(),
            )
            Box(
                Modifier.fillMaxWidth().height(130.dp)
                    .background(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.75f), Color.Transparent)))
            )
            Row(
                Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(Icons.AutoMirrored.Rounded.ArrowBack) { onBack() }
                Spacer(Modifier.weight(1f))
                Chip(trip.mode.label, bg = Lime, icon = trip.mode.icon())
                if (trip.auto) {
                    Spacer(Modifier.width(6.dp))
                    Chip("Auto", bg = Snow, icon = Icons.Rounded.AutoAwesome)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth()
                .padding(top = 0.dp)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(Paper)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Reveal(100) {
                Column {
                    SectionLabel(Format.fullDate(trip.start))
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        val (v, u) = Format.durationParts(trip.durationMs)
                        Text(v, style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.4f), color = Ink)
                        Spacer(Modifier.width(8.dp))
                        Text(u, style = MaterialTheme.typography.headlineMedium, color = Muted, modifier = Modifier.padding(bottom = 14.dp))
                        Spacer(Modifier.weight(1f))
                        if (sameMode.count > 0) {
                            val diff = ((trip.durationMs - sameMode.avgTime) / 60000).toInt()
                            Chip(
                                when {
                                    diff == 0 -> "On your avg"
                                    diff < 0 -> "${abs(diff)} min faster than avg"
                                    else -> "$diff min slower than avg"
                                },
                                bg = if (diff <= 0) Lime else Snow,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // origin → destination
            Reveal(200) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(18.dp)
                ) {
                    Stop("O", trip.fromName, Format.time(trip.start), origin = true) { endAction = 0 }
                    Row {
                        Spacer(Modifier.width(19.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(Muted.copy(alpha = 0.5f))) }
                        }
                    }
                    Stop("X", trip.toName, Format.time(trip.end), origin = false) { endAction = 1 }
                }
            }
            Spacer(Modifier.height(10.dp))

            Reveal(280) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val (dv, du) = Format.distanceParts(trip.distance)
                        StatTile(dv, du, "Distance", Modifier.weight(1f))
                        StatTile("${Format.kmh(trip.avgSpeed)}", "km/h", "Avg speed", Modifier.weight(1f), lime = true, icon = Icons.Rounded.Speed)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("${Format.kmh(trip.maxSpeed)}", "km/h", "Top speed", Modifier.weight(1f), dark = true, icon = Icons.Rounded.Bolt)
                        if (trip.mode == TripMode.WALK) {
                            StatTile("%,d".format(trip.steps), null, "Steps (est.)", Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.DirectionsWalk)
                        } else {
                            StatTile("${stops.size}", if (stops.size == 1) "stop" else "stops", "Signals & halts · ${Format.duration(Stops.totalMs(stops))}", Modifier.weight(1f))
                        }
                    }
                    val cost = Prefs.fuelCost(trip.mode, trip.distance)
                    if (cost != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("₹" + String.format(java.util.Locale.US, "%.0f", cost), null, "Fuel (est.)", Modifier.weight(1f), icon = Icons.Rounded.LocalGasStation)
                            val moving = (trip.durationMs - Stops.totalMs(stops)).coerceAtLeast(0)
                            val (mv, mu) = Format.durationParts(moving)
                            StatTile(mv, mu, "Actually moving", Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            if (stops.isNotEmpty()) {
                Reveal(320) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Snow).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Stops & traffic signals", color = Ink)
                            Spacer(Modifier.weight(1f))
                            Text("${Format.duration(Stops.totalMs(stops))} waiting", style = MaterialTheme.typography.labelMedium, color = Muted)
                        }
                        Spacer(Modifier.height(12.dp))
                        stops.forEachIndexed { i, st ->
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(30.dp).clip(CircleShape).background(Amber), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = Ink)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(Format.time(st.start), style = MaterialTheme.typography.titleSmall, color = Ink, modifier = Modifier.weight(1f))
                                Text(Format.clock(st.durationMs), style = MaterialTheme.typography.titleSmall, color = Ink)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Numbers match the amber dots on the map.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (trip.points.size > 3) {
                Reveal(360) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Ink).padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Speed", color = Snow.copy(alpha = 0.5f))
                            Spacer(Modifier.weight(1f))
                            Text("peak ${Format.speed(trip.maxSpeed)}", style = MaterialTheme.typography.labelMedium, color = Lime)
                        }
                        Spacer(Modifier.height(14.dp))
                        SpeedGraph(trip.points, Modifier.fillMaxWidth().height(150.dp))
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Text(Format.time(trip.start), style = MaterialTheme.typography.labelSmall, color = Snow.copy(alpha = 0.45f))
                            Spacer(Modifier.weight(1f))
                            Text(Format.time(trip.end), style = MaterialTheme.typography.labelSmall, color = Snow.copy(alpha = 0.45f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.pressable { confirmDelete = true }.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Snow).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Danger)
                Spacer(Modifier.width(8.dp))
                Text("Delete trip", style = MaterialTheme.typography.titleMedium, color = Danger)
            }
            Spacer(Modifier.height(30.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    endAction?.let { which ->
        EndActionsDialog(
            label = if (which == 0) "Origin · O" else "Destination · X",
            name = if (which == 0) trip.fromName else trip.toName,
            onDismiss = { endAction = null },
            onSavePlace = { endAction = null; savePlaceFor = which },
            onRename = { endAction = null; renaming = true },
        )
    }
    savePlaceFor?.let { which ->
        val p = if (which == 0) trip.points.firstOrNull() else trip.points.lastOrNull()
        SavePlaceDialog(
            title = if (which == 0) "Save the start as a place" else "Save the finish as a place",
            initialName = "",
            onDismiss = { savePlaceFor = null },
        ) { name, kind ->
            if (p != null) Places.add(name, kind, p.lat, p.lon)
            savePlaceFor = null
        }
    }
    if (renaming) {
        RenameTripDialog(trip.fromName, trip.toName, onDismiss = { renaming = false }) { a, b ->
            renaming = false
            scope.launch { TripRepo.save(trip.copy(origin = a, destination = b, renamed = true)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Snow,
            shape = RoundedCornerShape(32.dp),
            title = { Text("Delete this trip?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("It will be removed from your history and weekly stats.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    TripRepo.delete(trip.id)
                    onBack()
                }) { Text("Delete", color = Danger, style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Keep", color = Ink, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

private fun stoppedMs(trip: Trip): Long {
    var ms = 0L
    for (i in 1 until trip.points.size) {
        val a = trip.points[i - 1]; val b = trip.points[i]
        val gap = b.t - a.t
        when {
            gap > 8000 -> ms += gap - 3000 // no movement recorded = waiting
            b.speed < 1.2f && a.speed < 1.2f -> ms += gap
        }
    }
    return ms
}

@Composable
private fun Stop(badge: String, name: String, time: String, origin: Boolean, onClick: () -> Unit) {
    Row(Modifier.pressable(pressedScale = 0.98f, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(if (origin) Ink else Lime),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, style = MaterialTheme.typography.titleMedium, color = if (origin) Lime else Ink)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            SectionLabel(if (origin) "Origin" else "Destination")
            Text(name, style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(time, style = MaterialTheme.typography.labelLarge, color = Muted)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.Edit, "Edit", tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun TripDetailScreen(id: String, onBack: () -> Unit) {
    val places by com.frontpagestudios.ox.data.Places.places.collectAsState()
    androidx.compose.runtime.key(places) { TripDetailScreenBody(id, onBack) }
}
