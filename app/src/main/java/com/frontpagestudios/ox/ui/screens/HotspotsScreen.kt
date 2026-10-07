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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frontpagestudios.ox.data.TripPoint
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.ui.components.Chip
import com.frontpagestudios.ox.ui.components.CircleButton
import com.frontpagestudios.ox.ui.components.EmptyRouteArt
import com.frontpagestudios.ox.ui.components.OxMap
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.StatTile
import com.frontpagestudios.ox.ui.theme.Amber
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Paper
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Geo
import com.frontpagestudios.ox.util.Hotspots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

@Composable
fun HotspotsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val trips by TripRepo.trips.collectAsState()
    val spots = remember(trips) { Hotspots.build(trips).take(15) }
    val geo = remember(spots) { spots.map { GeoPoint(it.lat, it.lon) } }
    val names = remember { mutableStateMapOf<Int, String>() }
    LaunchedEffect(spots) {
        spots.take(8).forEachIndexed { i, s ->
            val n = withContext(Dispatchers.IO) { Geo.place(ctx, TripPoint(s.lat, s.lon, 0, 0f)) }
            if (n != null) names[i] = n
        }
    }

    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(380.dp).background(Ink)) {
            if (geo.isNotEmpty()) {
                OxMap(points = emptyList(), modifier = Modifier.fillMaxSize(), showEnds = false, fitKey = spots.size, fitPoints = geo, stops = geo)
            } else {
                EmptyRouteArt(Modifier.fillMaxSize(), "No stops recorded yet")
            }
            Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.75f), Color.Transparent))))
            Row(Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CircleButton(Icons.AutoMirrored.Rounded.ArrowBack) { onBack() }
                Spacer(Modifier.weight(1f))
                Chip("Signal hotspots", bg = Amber)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(22.dp))
            Text("Where you\nwait the most", style = MaterialTheme.typography.displayMedium, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "Every halt from all your rides and drives, grouped by spot. Numbers match the map.",
                style = MaterialTheme.typography.bodyMedium, color = Muted,
            )
            Spacer(Modifier.height(14.dp))
            val total = spots.sumOf { it.totalMs }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val (tv, tu) = Format.durationParts(total)
                StatTile(tv, tu, "Waiting at top spots", Modifier.weight(1f), dark = true)
                StatTile("${spots.sumOf { it.hits }}", null, "Stops counted", Modifier.weight(1f), lime = true)
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("Ranked by total waiting")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                spots.forEachIndexed { i, s ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Snow).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Amber), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = Ink)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(names[i] ?: "Spot ${i + 1}", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Text(
                                "${s.hits} stops on ${s.tripCount} trips · avg ${Format.clock(s.avgMs)}",
                                style = MaterialTheme.typography.bodySmall, color = Muted,
                            )
                        }
                        Text(Format.duration(s.totalMs), style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
