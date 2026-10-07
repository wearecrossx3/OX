package com.frontpagestudios.ox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.tracking.TrackingState
import com.frontpagestudios.ox.ui.Routes
import com.frontpagestudios.ox.ui.components.Chip
import com.frontpagestudios.ox.ui.components.EmptyRouteArt
import com.frontpagestudios.ox.ui.components.LimePill
import com.frontpagestudios.ox.ui.components.ModeSelector
import com.frontpagestudios.ox.ui.components.OxMap
import com.frontpagestudios.ox.ui.components.PulseDot
import com.frontpagestudios.ox.ui.components.Reveal
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.StatTile
import com.frontpagestudios.ox.ui.components.icon
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.startTrip
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Geo
import com.frontpagestudios.ox.util.Perms
import kotlinx.coroutines.delay

@Composable
private fun HomeTabBody(nav: NavHostController, openTab: (Int) -> Unit) {
    val ctx = LocalContext.current
    val trips by TripRepo.trips.collectAsState()
    val live by TrackingState.live.collectAsState()
    val name by Prefs.name.collectAsState()
    val vehicle by Prefs.vehicle.collectAsState()
    val autoOn by Prefs.autoDetect.collectAsState()
    var mode by remember { mutableStateOf(vehicle) }

    val week = remember(trips) { weekSummary(trips, 0) { it.mode.isVehicle } }
    val weekAll = remember(trips) { weekSummary(trips, 0) }
    val last = trips.firstOrNull()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        // top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Ink),
                contentAlignment = Alignment.Center,
            ) { com.frontpagestudios.ox.ui.components.OxLogo(10.dp, Lime) }
            Spacer(Modifier.weight(1f))
            Chip(Format.fullDate(System.currentTimeMillis()), bg = Snow)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.pressable { openTab(4) }.size(46.dp).clip(CircleShape).background(Lime),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (name.trim().firstOrNull()?.uppercase() ?: "☺"),
                    style = MaterialTheme.typography.titleMedium, color = Ink,
                )
            }
        }
        Spacer(Modifier.height(26.dp))
        Reveal(0) {
            Text(
                Format.greeting() + "\n" + (name.trim().ifEmpty { "there" }),
                style = MaterialTheme.typography.displayLarge, color = Ink,
            )
        }
        Spacer(Modifier.height(10.dp))
        Reveal(120) {
            Text(
                if (week.count > 0) "Avg trip this week: ${Format.duration(week.avgTime)}"
                else "Your trips will show up here.",
                style = MaterialTheme.typography.bodyLarge, color = Muted,
            )
        }
        Spacer(Modifier.height(22.dp))

        // live banner
        live?.let { l ->
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
            Row(
                Modifier.pressable { nav.navigate(Routes.LIVE) { launchSingleTop = true } }
                    .fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Ink).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseDot()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (l.auto) "Auto trip recording" else "Trip recording",
                        style = MaterialTheme.typography.titleMedium, color = Snow,
                    )
                    Text(
                        "${Format.clock(now - l.start)} · ${Format.distance(l.distance)} · ${l.mode.label}",
                        style = MaterialTheme.typography.bodySmall, color = Snow.copy(alpha = 0.6f),
                    )
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Lime),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.NorthEast, null, tint = Ink) }
            }
            Spacer(Modifier.height(14.dp))
        }

        // last trip card
        Reveal(200) {
            Box(
                Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(40.dp)).background(Ink)
            ) {
                if (last != null && last.points.size > 1) {
                    val pts = remember(last.id) { Geo.geo(last.points) }
                    OxMap(pts, Modifier.matchParentSize(), interactive = false, fitKey = last.id, lineWidthDp = 4f)
                    Box(
                        Modifier.matchParentSize().pressable(pressedScale = 0.98f) { nav.navigate(Routes.trip(last.id)) }
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Ink.copy(alpha = 0.85f))))
                    )
                    Row(Modifier.align(Alignment.TopStart).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Last trip", bg = Lime)
                        Chip(Format.day(last.start), bg = Snow)
                    }
                    Row(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                last.title, style = MaterialTheme.typography.titleLarge, color = Snow,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${Format.distance(last.distance)} · avg ${Format.kmh(last.avgSpeed)} km/h",
                                style = MaterialTheme.typography.bodySmall, color = Snow.copy(alpha = 0.6f),
                            )
                        }
                        Text(Format.duration(last.durationMs), style = MaterialTheme.typography.headlineMedium, color = Lime)
                    }
                } else {
                    EmptyRouteArt(Modifier.matchParentSize(), "Your first trip will draw itself here")
                    Chip("No trips yet", bg = Lime, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        // start
        Reveal(280) {
            Column {
                ModeSelector(mode, { mode = it })
                Spacer(Modifier.height(10.dp))
                LimePill(
                    title = if (live != null) "Open live trip" else "Start ${if (mode == TripMode.WALK) "walk" else "trip"}",
                    subtitle = if (live != null) "Recording now" else if (autoOn && Perms.canAuto(ctx)) "Manual · auto-detect is also on" else "Manual · ${mode.label}",
                    sound = null,
                    icon = if (live != null) Icons.Rounded.NorthEast else mode.icon(),
                ) { startTrip(ctx, nav, mode) }
            }
        }
        Spacer(Modifier.height(14.dp))

        // usual route
        val usual = remember(trips) { com.frontpagestudios.ox.data.RouteBook.groups(trips).firstOrNull { it.count >= 2 } }
        if (usual != null) {
            Reveal(320) {
                RouteCard(usual, featured = true) { nav.navigate(routeLink(usual.key)) }
            }
            Spacer(Modifier.height(10.dp))
        }

        // tiles
        Reveal(360) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        "${weekAll.count}", null, "Trips this week",
                        Modifier.weight(1f), onClick = { openTab(1) },
                    )
                    val (v, u) = Format.durationParts(week.avgTime)
                    StatTile(
                        v, u, "Avg commute time", Modifier.weight(1f), dark = true,
                        icon = Icons.Rounded.Timer, onClick = { openTab(3) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val (dv, du) = Format.distanceParts(weekAll.totalDist)
                    StatTile(dv, du, "Distance this week", Modifier.weight(1f), onClick = { openTab(3) })
                    val walks = weekAll.trips.count { it.mode == TripMode.WALK }
                    StatTile(
                        "$walks", null, "Walks this week", Modifier.weight(1f), lime = true,
                        icon = Icons.AutoMirrored.Rounded.DirectionsWalk, onClick = { startTrip(ctx, nav, TripMode.WALK) },
                    )
                }
                Row(
                    Modifier.pressable { openTab(4) }.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Ink)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        val ready = autoOn && Perms.canAuto(ctx)
                        Text(
                            if (ready) "Auto-detect is on" else "Auto-detect is off",
                            style = MaterialTheme.typography.titleMedium, color = Ink,
                        )
                        Text(
                            if (ready) "Trips start when you ride or drive" else "Tap to turn it on in settings",
                            style = MaterialTheme.typography.bodySmall, color = Muted,
                        )
                    }
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(if (autoOn && Perms.canAuto(ctx)) Lime else Muted.copy(alpha = 0.4f))
                    )
                }
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
fun HomeTab(nav: NavHostController, openTab: (Int) -> Unit) {
    val places by com.frontpagestudios.ox.data.Places.places.collectAsState()
    androidx.compose.runtime.key(places) { HomeTabBody(nav, openTab) }
}
