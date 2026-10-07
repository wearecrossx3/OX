package com.frontpagestudios.ox.ui.screens

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.RouteBook
import com.frontpagestudios.ox.data.Trip
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.ui.components.CircleButton
import com.frontpagestudios.ox.ui.components.LimePill
import com.frontpagestudios.ox.ui.components.OxLogo
import com.frontpagestudios.ox.ui.components.RouteShape
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.theme.Graphite
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Txt
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Paper
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Stops
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Month(val label: String, val trips: List<Trip>)

private fun month(all: List<Trip>, offset: Int): Month {
    val c = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MONTH, offset)
    }
    val s = c.timeInMillis
    c.add(Calendar.MONTH, 1)
    val e = c.timeInMillis
    return Month(SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(s)), all.filter { it.start in s until e })
}

@Composable
fun RecapScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val trips by TripRepo.trips.collectAsState()
    var offset by remember { mutableIntStateOf(0) }
    val m = remember(trips, offset) { month(trips, offset) }
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    val km = m.trips.sumOf { it.distance }
    val moving = m.trips.sumOf { it.durationMs }
    val vehicle = m.trips.filter { it.mode.isVehicle }
    val stops = remember(m) { vehicle.map { Stops.find(it.points) } }
    val waits = stops.sumOf { Stops.totalMs(it) }
    val fuel = m.trips.mapNotNull { Prefs.fuelCost(it.mode, it.distance) }.sum()
    val top = remember(m) { RouteBook.groups(m.trips).firstOrNull() }
    val longest = m.trips.maxByOrNull { it.distance }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(Icons.AutoMirrored.Rounded.ArrowBack) { onBack() }
            Spacer(Modifier.weight(1f))
            CircleButton(Icons.AutoMirrored.Rounded.ArrowBack, size = 46.dp, bg = Mist) { offset-- }
            Spacer(Modifier.width(8.dp))
            CircleButton(Icons.AutoMirrored.Rounded.ArrowForward, size = 46.dp, bg = if (offset < 0) Ink else Mist, fg = if (offset < 0) Lime else Muted) {
                if (offset < 0) offset++
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Monthly\nrecap", style = MaterialTheme.typography.displayLarge, color = Txt)
        Spacer(Modifier.height(16.dp))

        // the shareable card
        Column(
            Modifier
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
                .fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Ink).padding(26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OxLogo(16.dp, Lime)
                Spacer(Modifier.weight(1f))
                Text(m.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = Snow.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(26.dp))
            val (kv, ku) = Format.distanceParts(km)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(kv, style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f), color = Lime)
                Spacer(Modifier.width(8.dp))
                Text(ku, style = MaterialTheme.typography.headlineMedium, color = Lime.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 14.dp))
            }
            Text("travelled in ${m.trips.size} trips", style = MaterialTheme.typography.bodyLarge, color = Snow.copy(alpha = 0.7f))
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecapCell("On the move", Format.duration(moving), Modifier.weight(1f))
                RecapCell("Signals & stops", "${stops.sumOf { it.size }}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecapCell("Waiting", Format.duration(waits), Modifier.weight(1f))
                RecapCell("Fuel (est.)", if (fuel > 0) "₹" + String.format(Locale.US, "%.0f", fuel) else "—", Modifier.weight(1f))
            }
            if (top != null) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(64.dp).height(64.dp).clip(RoundedCornerShape(18.dp)).background(Graphite)) {
                        RouteShape(top.trips.first().points, Modifier.fillMaxSize(), stroke = 4f)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        SectionLabel("Top route · ${top.count}×", color = Snow.copy(alpha = 0.5f))
                        Text("${top.from} → ${top.to}", style = MaterialTheme.typography.titleLarge, color = Snow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("avg ${Format.duration(top.avgMs)}", style = MaterialTheme.typography.bodySmall, color = Lime)
                    }
                }
            }
            if (longest != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Longest: ${longest.title} · ${Format.distance(longest.distance)}",
                    style = MaterialTheme.typography.bodySmall, color = Snow.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("OX · by Frontpage Studios", style = MaterialTheme.typography.labelMedium, color = Snow.copy(alpha = 0.4f))
        }

        Spacer(Modifier.height(16.dp))
        LimePill("Share recap", "Image for Instagram or WhatsApp", icon = Icons.Rounded.IosShare) {
            scope.launch {
                val bmp = layer.toImageBitmap().asAndroidBitmap()
                val uri = withContext(Dispatchers.IO) {
                    val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
                    val f = File(dir, "ox-recap.png")
                    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
                }
                val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(Intent.createChooser(send, "Share OX recap"))
            }
        }
        Spacer(Modifier.height(30.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun RecapCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Graphite).padding(14.dp)) {
        SectionLabel(label, color = Snow.copy(alpha = 0.45f))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Snow, maxLines = 1)
    }
}
