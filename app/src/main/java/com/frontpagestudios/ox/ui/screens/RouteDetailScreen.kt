package com.frontpagestudios.ox.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.frontpagestudios.ox.data.Places
import com.frontpagestudios.ox.data.RouteBook
import com.frontpagestudios.ox.data.RouteGroup
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.ui.Routes
import com.frontpagestudios.ox.ui.components.Chip
import com.frontpagestudios.ox.ui.components.CircleButton
import com.frontpagestudios.ox.ui.components.OxMap
import com.frontpagestudios.ox.ui.components.Reveal
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.StatTile
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Paper
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Geo
import java.util.Locale
import kotlin.math.max

@Composable
fun RouteDetailScreen(routeKey: String, nav: NavHostController, onBack: () -> Unit) {
    val places by Places.places.collectAsState()
    key(places) {
        val trips by TripRepo.trips.collectAsState()
        val group = remember(trips, routeKey) { RouteBook.find(trips, routeKey) }
        if (group == null) {
            Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
                Chip("Route not found · go back", bg = Lime, modifier = Modifier.pressable { onBack() })
            }
        } else {
            RouteDetail(group, nav, onBack)
        }
    }
}

private fun slotLabel(min: Int): String {
    val h = min / 60; val m = min % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return String.format(Locale.US, "%d:%02d %s", h12, m, if (h < 12) "am" else "pm")
}

@Composable
private fun RouteDetail(g: RouteGroup, nav: NavHostController, onBack: () -> Unit) {
    val latest = g.trips.first()
    val main = remember(g.key, latest.id) { Geo.geo(latest.points) }
    val others = remember(g.key, g.count) { g.trips.drop(1).take(20).map { Geo.geo(it.points) } }
    val chrono = remember(g.key, g.count) { g.trips.take(14).reversed() }
    val slots = remember(g.key, g.count) { RouteBook.departureSlots(g) }
    val bestSlot = slots.minByOrNull { it.second }

    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(380.dp).background(Ink)) {
            OxMap(
                points = main, modifier = Modifier.fillMaxSize(), fitKey = g.key, fitPoints = main + others.flatten(),
                extraLines = others, extraKey = g.key + g.count, lineWidthDp = 5f,
            )
            Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.75f), Color.Transparent))))
            Row(Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CircleButton(Icons.AutoMirrored.Rounded.ArrowBack) { onBack() }
                Spacer(Modifier.weight(1f))
                Chip("${g.count} trips overlaid", bg = Lime)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(22.dp))
            Reveal(80) {
                Column {
                    SectionLabel("Route")
                    Text("${g.from} → ${g.to}", style = MaterialTheme.typography.headlineLarge, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        val (v, u) = Format.durationParts(g.avgMs)
                        Text(v, style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.3f), color = Ink)
                        Spacer(Modifier.width(8.dp))
                        Text("$u avg", style = MaterialTheme.typography.headlineSmall, color = Muted, modifier = Modifier.padding(bottom = 12.dp))
                        Spacer(Modifier.weight(1f))
                        val diff = ((latest.durationMs - g.avgMs) / 60000).toInt()
                        if (g.count > 1) Chip(
                            when {
                                diff == 0 -> "Last trip on avg"
                                diff < 0 -> "Last: ${-diff} min faster"
                                else -> "Last: $diff min slower"
                            },
                            bg = if (diff <= 0) Lime else Snow, modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Format.duration(g.best?.durationMs ?: 0), null, "Best time", Modifier.weight(1f), lime = true, icon = Icons.Rounded.Bolt,
                    onClick = { g.best?.let { nav.navigate(Routes.trip(it.id)) } })
                StatTile(Format.duration(g.worst?.durationMs ?: 0), null, "Slowest", Modifier.weight(1f), dark = true,
                    onClick = { g.worst?.let { nav.navigate(Routes.trip(it.id)) } })
            }

            val stopStats = remember(g.key, g.count) {
                val per = g.trips.filter { it.mode.isVehicle }.map { com.frontpagestudios.ox.util.Stops.find(it.points) }
                if (per.isEmpty()) null else
                    (per.sumOf { it.size }.toFloat() / per.size) to (per.sumOf { com.frontpagestudios.ox.util.Stops.totalMs(it) } / per.size)
            }
            if (stopStats != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(String.format(Locale.US, "%.1f", stopStats.first), null, "Avg stops / signals", Modifier.weight(1f))
                    StatTile(Format.duration(stopStats.second), null, "Avg time waiting", Modifier.weight(1f))
                }
            }

            if (chrono.size >= 2) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Ink).padding(22.dp)) {
                    Row {
                        SectionLabel("Trip by trip", color = Snow.copy(alpha = 0.5f))
                        Spacer(Modifier.weight(1f))
                        Text("lime = best", style = MaterialTheme.typography.labelMedium, color = Lime)
                    }
                    Spacer(Modifier.height(14.dp))
                    TripBars(chrono.map { it.durationMs / 60000f }, (g.best?.let { chrono.indexOf(it) } ?: -1), Modifier.fillMaxWidth().height(140.dp))
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(Format.day(chrono.first().start), style = MaterialTheme.typography.labelSmall, color = Snow.copy(alpha = 0.45f))
                        Spacer(Modifier.weight(1f))
                        Text(Format.day(chrono.last().start), style = MaterialTheme.typography.labelSmall, color = Snow.copy(alpha = 0.45f))
                    }
                }
            }

            if (slots.size >= 2 && bestSlot != null) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Snow).padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Rounded.Schedule, null, tint = Ink)
                        Spacer(Modifier.width(8.dp))
                        SectionLabel("Best time to leave", color = Ink)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Leave around ${slotLabel(bestSlot.first)} — avg ${Format.duration(bestSlot.second)}",
                        style = MaterialTheme.typography.headlineSmall, color = Ink,
                    )
                    Spacer(Modifier.height(16.dp))
                    val maxV = slots.maxOf { it.second }.toFloat()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slots.forEach { (slot, ms) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(slotLabel(slot), style = MaterialTheme.typography.labelMedium, color = Muted, modifier = Modifier.width(70.dp))
                                Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(50)).background(com.frontpagestudios.ox.ui.theme.Mist)) {
                                    Box(
                                        Modifier.fillMaxWidth((ms / maxV).coerceIn(0.05f, 1f)).height(14.dp).clip(RoundedCornerShape(50))
                                            .background(if (slot == bestSlot.first) Lime else Ink)
                                    )
                                }
                                Text(Format.duration(ms), style = MaterialTheme.typography.labelMedium, color = Ink, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("All trips on this route")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                g.trips.forEach { t -> TripRow(t) { nav.navigate(Routes.trip(t.id)) } }
            }
            Spacer(Modifier.height(30.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun TripBars(values: List<Float>, best: Int, modifier: Modifier = Modifier) {
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        val n = values.size
        val gap = size.width * 0.03f
        val bw = (size.width - gap * (n - 1)) / n
        val vmax = max(values.maxOrNull() ?: 1f, 1f)
        values.forEachIndexed { i, v ->
            val h = (v / vmax * size.height * progress.value).coerceAtLeast(bw.coerceAtMost(8f))
            drawRoundRect(
                if (i == best) Lime else Color.White.copy(alpha = if (i == n - 1) 0.95f else 0.55f),
                Offset(i * (bw + gap), size.height - h), Size(bw, h), CornerRadius(minOf(bw / 2, 14f)),
            )
        }
    }
}
