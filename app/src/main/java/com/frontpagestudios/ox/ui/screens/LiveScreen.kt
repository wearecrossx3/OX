package com.frontpagestudios.ox.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.tracking.TrackingService
import com.frontpagestudios.ox.tracking.TrackingState
import com.frontpagestudios.ox.ui.components.AnimatedNumber
import com.frontpagestudios.ox.ui.components.CircleButton
import com.frontpagestudios.ox.ui.components.ModeSelector
import com.frontpagestudios.ox.ui.components.OxMap
import com.frontpagestudios.ox.ui.components.OxLogo
import androidx.compose.ui.unit.sp
import com.frontpagestudios.ox.ui.components.PulseDot
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.theme.Graphite
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Sfx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun LiveScreen(onClose: () -> Unit, onSaved: (String) -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val live by TrackingState.live.collectAsState()
    val saving by TrackingState.saving.collectAsState()
    val lastSaved by TrackingState.lastSaved.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var follow by remember { mutableStateOf(true) }
    var recenter by remember { mutableIntStateOf(0) }
    var stopping by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // light status bar icons on this dark screen
    DisposableEffect(Unit) {
        val window = ctx.findActivity()?.window
        val ctl = window?.let { WindowCompat.getInsetsController(it, view) }
        ctl?.isAppearanceLightStatusBars = false
        onDispose { ctl?.isAppearanceLightStatusBars = true }
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    // trip finished (by us, or auto-stop while watching)
    LaunchedEffect(lastSaved) {
        val id = lastSaved ?: return@LaunchedEffect
        TrackingState.lastSaved.value = null
        if (id.isEmpty()) {
            Toast.makeText(ctx, "Trip was too short, so it wasn't saved", Toast.LENGTH_SHORT).show()
            onClose()
        } else {
            Sfx.play(Sfx.S.SUCCESS)
            onSaved(id)
        }
    }
    // nothing running and nothing coming: leave
    LaunchedEffect(live == null, saving) {
        if (live == null && !saving) {
            delay(5000)
            if (TrackingState.live.value == null && !TrackingState.saving.value) onClose()
        }
    }

    // every-km ping
    val km = ((live?.distance ?: 0.0) / 1000).toInt()
    var lastKm by remember { mutableIntStateOf(-1) }
    LaunchedEffect(km) {
        if (lastKm in 0 until km) {
            Sfx.play(Sfx.S.PING)
            if (Prefs.haptics.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastKm = km
    }

    BackHandler { onClose() }

    val l = live
    val pts = remember(l?.points?.size) { l?.points?.map { GeoPoint(it.lat, it.lon) } ?: emptyList() }
    val here = l?.current?.let { GeoPoint(it.lat, it.lon) }
    val liveStops = remember(l?.points?.size) {
        if (l != null && l.mode.isVehicle) com.frontpagestudios.ox.util.Stops.find(l.points) else emptyList()
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        OxMap(
            points = pts,
            modifier = Modifier.fillMaxSize(),
            follow = if (follow) here else null,
            liveDot = here,
            recenterKey = recenter,
            fitKey = if (follow) null else "free",
            lineWidthDp = 6f,
        )
        // top fade + controls
        Box(
            Modifier.fillMaxWidth().height(150.dp)
                .background(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.8f), Color.Transparent)))
        )
        Row(
            Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(Icons.Rounded.KeyboardArrowDown, bg = Snow) { onClose() }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Ink).padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseDot(size = 8.dp)
                Text(
                    when {
                        l == null -> "STARTING"
                        l.points.isEmpty() -> "FINDING GPS"
                        l.auto -> "AUTO · REC"
                        else -> "REC"
                    },
                    style = MaterialTheme.typography.labelLarge, color = Snow,
                )
            }
            Spacer(Modifier.weight(1f))
            CircleButton(
                if (follow) Icons.Rounded.GpsFixed else Icons.Rounded.GpsNotFixed,
                bg = if (follow) Lime else Snow,
            ) {
                follow = !follow
                if (follow) recenter++
            }
        }

        // bottom panel
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)).background(Ink)
                .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedNumber(
                    Format.kmh(l?.speed ?: 0f).toFloat(),
                    MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.7f),
                    Snow, durationMs = 700,
                )
                Spacer(Modifier.width(8.dp))
                Text("km/h", style = MaterialTheme.typography.headlineSmall, color = Lime, modifier = Modifier.padding(bottom = 18.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 16.dp)) {
                    SectionLabel("Top", color = Snow.copy(alpha = 0.45f))
                    Text("${Format.kmh(l?.maxSpeed ?: 0f)}", style = MaterialTheme.typography.headlineMedium, color = Snow)
                    if (l != null && l.mode.isVehicle) {
                        Text(
                            "${liveStops.size} stop${if (liveStops.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium, color = com.frontpagestudios.ox.ui.theme.Amber,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Time", Format.clock(if (l != null) now - l.start else 0), Modifier.weight(1.2f))
                val (dv, du) = Format.distanceParts(l?.distance ?: 0.0)
                Metric("Distance", "$dv $du", Modifier.weight(1f))
                Metric("Avg", "${Format.kmh(l?.avgSpeed ?: 0.0)} km/h", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            if (l != null) {
                ModeSelector(l.mode, { TrackingState.setMode(it) }, dark = true)
                Spacer(Modifier.height(10.dp))
            }
            HoldToFinish(enabled = l != null && !stopping) {
                stopping = true
                Sfx.play(Sfx.S.STOP)
                TrackingService.stop(ctx)
            }
        }

        AnimatedVisibility(saving || stopping, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Ink.copy(alpha = 0.92f))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OxLogo(46.dp, Lime)
                    Spacer(Modifier.height(6.dp))
                    PulseDot()
                    Spacer(Modifier.height(18.dp))
                    Text("Saving your trip…", style = MaterialTheme.typography.headlineSmall, color = Snow)
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Graphite).padding(horizontal = 14.dp, vertical = 12.dp)) {
        SectionLabel(label, color = Snow.copy(alpha = 0.45f))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Snow, maxLines = 1)
    }
}

@Composable
private fun HoldToFinish(enabled: Boolean, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(37.dp)).background(Graphite)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    if (Prefs.haptics.value) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val job = scope.launch {
                        progress.animateTo(1f, tween(1100, easing = LinearEasing))
                        if (Prefs.haptics.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDone()
                    }
                    tryAwaitRelease()
                    if (progress.value < 1f) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(260)) }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(progress.value)
                .clip(RoundedCornerShape(37.dp)).background(Lime)
        )
        Text(
            if (progress.value > 0f) "Keep holding…" else "Hold to finish trip",
            style = MaterialTheme.typography.titleMedium,
            color = if (progress.value > 0.5f) Ink else Snow,
        )
    }
}
