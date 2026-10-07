package com.frontpagestudios.ox.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.frontpagestudios.ox.ui.components.EmptyRouteArt
import com.frontpagestudios.ox.ui.components.RouteShape
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.icon
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Card
import com.frontpagestudios.ox.ui.theme.Txt
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format

private val filters = listOf("All", "Car", "Bike", "Walk", "Auto")

private fun Trip.matches(f: Int) = when (f) {
    1 -> mode == TripMode.CAR
    2 -> mode == TripMode.BIKE
    3 -> mode == TripMode.WALK
    4 -> auto
    else -> true
}

@Composable
private fun HistoryTabBody(nav: NavHostController) {
    val trips by TripRepo.trips.collectAsState()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val shown = remember(trips, filter) { trips.filter { it.matches(filter) } }
    val grouped = remember(shown) { shown.groupBy { Format.day(it.start) } }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Your\ntrips", style = MaterialTheme.typography.displayLarge, color = Txt)
            Spacer(Modifier.height(6.dp))
            Text(
                "${trips.size} recorded · ${Format.distance(trips.sumOf { it.distance })} total",
                style = MaterialTheme.typography.bodyLarge, color = Muted,
            )
            Spacer(Modifier.height(18.dp))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters.size) { i ->
                    val on = i == filter
                    val bg by animateColorAsState(if (on) Ink else Card, tween(220), label = "f")
                    Box(
                        Modifier.pressable { filter = i }.clip(RoundedCornerShape(50)).background(bg)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(filters[i], style = MaterialTheme.typography.labelLarge, color = if (on) Lime else Txt)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (shown.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(36.dp)).background(Ink)) {
                    EmptyRouteArt(Modifier.fillMaxSize(), if (trips.isEmpty()) "No trips yet — go for a ride" else "Nothing here for this filter")
                }
            }
        }
        grouped.forEach { (day, list) ->
            item(key = "h_$day") {
                Row(Modifier.padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(day, color = Txt)
                    Spacer(Modifier.width(8.dp))
                    SectionLabel("${list.size} · ${Format.duration(list.sumOf { it.durationMs })}")
                }
            }
            items(list, key = { it.id }) { trip ->
                TripRow(trip, Modifier.animateItem()) { nav.navigate(Routes.trip(trip.id)) }
            }
        }
    }
}

@Composable
fun TripRow(trip: Trip, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.pressable(pressedScale = 0.97f, onClick = onClick)
            .fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Card).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(78.dp).clip(RoundedCornerShape(24.dp)).background(Ink)) {
            RouteShape(trip.points, Modifier.fillMaxSize(), stroke = 4.5f)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                trip.title, style = MaterialTheme.typography.titleMedium, color = Txt,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${Format.time(trip.start)} – ${Format.time(trip.end)}",
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Mist),
                    contentAlignment = Alignment.Center,
                ) { Icon(trip.mode.icon(), null, tint = Txt, modifier = Modifier.size(15.dp)) }
                if (trip.auto) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(Lime),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.AutoAwesome, null, tint = Ink, modifier = Modifier.size(14.dp)) }
                }
                Spacer(Modifier.width(8.dp))
                Text(Format.distance(trip.distance), style = MaterialTheme.typography.labelMedium, color = Txt)
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 10.dp)) {
            val (v, u) = Format.durationParts(trip.durationMs)
            Text(v, style = MaterialTheme.typography.headlineLarge, color = Txt)
            Text(u, style = MaterialTheme.typography.labelMedium, color = Muted)
        }
    }
}

@Composable
fun HistoryTab(nav: NavHostController) {
    val places by com.frontpagestudios.ox.data.Places.places.collectAsState()
    androidx.compose.runtime.key(places) { HistoryTabBody(nav) }
}
