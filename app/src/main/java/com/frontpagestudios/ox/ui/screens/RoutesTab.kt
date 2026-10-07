package com.frontpagestudios.ox.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.frontpagestudios.ox.data.Places
import com.frontpagestudios.ox.data.RouteBook
import com.frontpagestudios.ox.data.RouteGroup
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.ui.Routes
import com.frontpagestudios.ox.ui.components.EmptyRouteArt
import com.frontpagestudios.ox.ui.components.RouteShape
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.Sparkline
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format

fun routeLink(key: String) = "route/" + Uri.encode(key)

@Composable
fun RoutesTab(nav: NavHostController, openSettings: () -> Unit) {
    val places by Places.places.collectAsState()
    key(places) { RoutesBody(nav, openSettings, places.isEmpty()) }
}

@Composable
private fun RoutesBody(nav: NavHostController, openSettings: () -> Unit, noPlaces: Boolean) {
    val trips by TripRepo.trips.collectAsState()
    val groups = remember(trips) { RouteBook.groups(trips) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Your\nroutes", style = MaterialTheme.typography.displayLarge, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Same start, same finish — grouped so you can see if you are getting faster.",
                style = MaterialTheme.typography.bodyLarge, color = Muted,
            )
            Spacer(Modifier.height(14.dp))
        }
        if (noPlaces) {
            item {
                Row(
                    Modifier.pressable { openSettings() }.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Lime).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PushPin, null, tint = Ink)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Save Home & Office", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(
                            "Named places make routes exact. Add them in Settings, or tap O / X on any trip.",
                            style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
        if (groups.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(36.dp)).background(Ink)) {
                    EmptyRouteArt(Modifier.fillMaxSize(), "Routes appear after a few trips")
                }
            }
        }
        itemsIndexed(groups, key = { _, g -> g.key }) { i, g ->
            RouteCard(g, featured = i == 0 && g.count > 1, modifier = Modifier.animateItem()) { nav.navigate(routeLink(g.key)) }
        }
    }
}

@Composable
fun RouteCard(g: RouteGroup, featured: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (featured) Ink else Snow
    val fg = if (featured) Snow else Ink
    val trend = remember(g.key, g.count) { g.trips.take(12).reversed().map { it.durationMs / 60000f } }
    Column(
        modifier.pressable(pressedScale = 0.97f, onClick = onClick).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(bg).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(if (featured) com.frontpagestudios.ox.ui.theme.Graphite else Ink)) {
                RouteShape(g.trips.first().points, Modifier.fillMaxSize(), stroke = 4f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (featured) SectionLabel("Your usual route", color = Lime)
                Text("${g.from} → ${g.to}", style = MaterialTheme.typography.titleLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${g.count} trip${if (g.count == 1) "" else "s"} · ~${Format.distance(g.avgDistance)}",
                    style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.55f),
                )
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (featured) Lime else com.frontpagestudios.ox.ui.theme.Mist), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.NorthEast, null, tint = Ink, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column {
                SectionLabel("Avg", color = fg.copy(alpha = 0.5f))
                Text(Format.duration(g.avgMs), style = MaterialTheme.typography.headlineMedium, color = if (featured) Lime else Ink)
            }
            Spacer(Modifier.width(22.dp))
            Column {
                SectionLabel("Best", color = fg.copy(alpha = 0.5f))
                Text(Format.duration(g.best?.durationMs ?: 0), style = MaterialTheme.typography.headlineMedium, color = fg)
            }
            Spacer(Modifier.weight(1f))
            if (trend.size >= 2) Sparkline(trend, Modifier.width(96.dp).height(40.dp), color = if (featured) Lime else Ink)
        }
    }
}
