package com.frontpagestudios.ox.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.tracking.AutoDetect
import com.frontpagestudios.ox.ui.components.Chip
import com.frontpagestudios.ox.ui.components.EmptyRouteArt
import com.frontpagestudios.ox.ui.components.ModeSelector
import com.frontpagestudios.ox.ui.components.OxMark
import com.frontpagestudios.ox.ui.components.Reveal
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.drawPartialPath
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Paper
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Perms
import com.frontpagestudios.ox.util.Sfx
import kotlinx.coroutines.launch

private val pageBg = listOf(Ink, Paper, Lime, Paper)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    val bg by animateColorAsState(pageBg[pager.currentPage], tween(450), label = "bg")
    val fg = if (pager.currentPage == 0) Snow else Ink

    Box(Modifier.fillMaxSize().background(bg)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = true) { page ->
            when (page) {
                0 -> IntroPage()
                1 -> AutoPage()
                2 -> WalkPage()
                else -> SetupPage(onDone)
            }
        }
        if (pager.currentPage < 3) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { i ->
                        val w by animateDpAsState(if (i == pager.currentPage) 26.dp else 8.dp, label = "dot")
                        Box(
                            Modifier.height(8.dp).width(w).clip(CircleShape)
                                .background(if (i == pager.currentPage) fg else fg.copy(alpha = 0.25f))
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.pressable(sound = Sfx.S.SWOOSH) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }.size(68.dp).clip(CircleShape).background(if (pager.currentPage == 0) Lime else Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward, null,
                        tint = if (pager.currentPage == 0) Ink else Lime, modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageText(title: String, body: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Reveal(80) { Text(title, style = MaterialTheme.typography.displayLarge, color = color) }
        Spacer(Modifier.height(16.dp))
        Reveal(220) { Text(body, style = MaterialTheme.typography.bodyLarge, color = color.copy(alpha = 0.65f)) }
    }
}

@Composable
private fun IntroPage() {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("OX", style = MaterialTheme.typography.headlineMedium, color = Lime)
            Spacer(Modifier.width(10.dp))
            Text("origin  ×  destination", style = MaterialTheme.typography.labelMedium, color = Snow.copy(alpha = 0.5f))
        }
        Spacer(Modifier.weight(0.6f))
        Reveal(0) { OxMark(Modifier.fillMaxWidth().height(130.dp)) }
        Spacer(Modifier.weight(0.5f))
        PageText(
            "Every route.\nMeasured.",
            "OX records your daily trips — office, gym, anywhere — and shows how long they really take.",
            Snow,
        )
        Spacer(Modifier.height(130.dp))
    }
}

@Composable
private fun AutoPage() {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(40.dp)).background(Ink)) {
                EmptyRouteArt(Modifier.fillMaxSize(), "Home → Office")
            }
            Reveal(300, Modifier.align(Alignment.TopEnd).padding(18.dp)) {
                Chip("24 min", bg = Lime, icon = Icons.Rounded.Timer)
            }
            Reveal(500, Modifier.align(Alignment.CenterStart).padding(18.dp)) {
                Chip("42 km/h avg", bg = Snow)
            }
        }
        Spacer(Modifier.height(28.dp))
        PageText(
            "Trips track\nthemselves.",
            "Hop on your bike or car and OX starts on its own. Prefer control? Start any trip manually.",
            Ink,
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun WalkPage() {
    val t = rememberInfiniteTransition(label = "walk")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "p")
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(24.dp))
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val w = size.width; val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.88f)
                cubicTo(w * 0.05f, h * 0.85f, w * 0.0f, h * 0.35f, w * 0.3f, h * 0.2f)
                cubicTo(w * 0.55f, h * 0.05f, w * 0.95f, h * 0.15f, w * 0.88f, h * 0.48f)
                cubicTo(w * 0.82f, h * 0.75f, w * 0.6f, h * 0.55f, w * 0.55f, h * 0.7f)
                cubicTo(w * 0.52f, h * 0.8f, w * 0.52f, h * 0.86f, w * 0.5f, h * 0.88f)
            }
            drawPartialPath(path, (p * 1.15f).coerceAtMost(1f), Ink, 7.dp.toPx())
            drawCircle(Ink, 9.dp.toPx(), Offset(w * 0.5f, h * 0.88f))
        }
        Spacer(Modifier.height(28.dp))
        PageText(
            "Walks draw\ntheir shape.",
            "Evening walk? Every step becomes a line on the map — distance, time and pace included.",
            Ink,
        )
        Spacer(Modifier.height(120.dp))
    }
}

private data class PermRow(val icon: ImageVector, val title: String, val body: String, val granted: Boolean, val enabled: Boolean, val ask: () -> Unit)

@Composable
private fun SetupPage(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val oneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    @Suppress("UNUSED_VARIABLE") val t = tick
    val loc = Perms.location(ctx)
    val bgLoc = Perms.background(ctx)
    val act = Perms.activity(ctx)
    val notif = Perms.notifications(ctx)

    val rows = listOf(
        PermRow(Icons.Rounded.LocationOn, "Location", "Draw your route and measure speed", loc, true) {
            locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        PermRow(Icons.Rounded.MyLocation, "Allow all the time", "Needed for auto trips while the app is closed", bgLoc, loc) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) oneLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        },
        PermRow(Icons.Rounded.DirectionsRun, "Physical activity", "Detects when you start driving or riding", act, true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) oneLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        },
        PermRow(Icons.Rounded.Notifications, "Notifications", "Live trip status and trip summaries", notif, true) {
            if (Build.VERSION.SDK_INT >= 33) oneLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
    )

    val name by Prefs.name.collectAsState()
    val vehicle by Prefs.vehicle.collectAsState()

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Text("Let's set\nyou up.", style = MaterialTheme.typography.displayLarge, color = Ink)
        Spacer(Modifier.height(28.dp))

        SectionLabel("What should we call you?")
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Snow).padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            if (name.isEmpty()) Text("Your name", style = MaterialTheme.typography.titleLarge, color = Muted)
            BasicTextField(
                value = name,
                onValueChange = { Prefs.setName(it.take(20)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(color = Ink),
                cursorBrush = SolidColor(Ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(22.dp))
        SectionLabel("Your daily ride")
        Spacer(Modifier.height(10.dp))
        ModeSelector(vehicle, { Prefs.setVehicle(it) }, modes = listOf(TripMode.BIKE, TripMode.CAR))

        Spacer(Modifier.height(26.dp))
        SectionLabel("Permissions")
        Spacer(Modifier.height(10.dp))
        rows.forEach { r ->
            PermissionItem(r)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))
        val ready = loc
        Row(
            Modifier
                .pressable(enabled = ready, sound = Sfx.S.SUCCESS) {
                    if (Perms.canAuto(ctx)) AutoDetect.enable(ctx)
                    onDone()
                }
                .fillMaxWidth().height(80.dp).clip(RoundedCornerShape(40.dp))
                .background(if (ready) Ink else Mist)
                .padding(start = 28.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (ready) "Start using OX" else "Allow location to continue",
                style = MaterialTheme.typography.headlineSmall,
                color = if (ready) Snow else Muted,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(if (ready) Lime else Snow),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Your trips stay on this phone. Nothing is uploaded.",
            style = MaterialTheme.typography.bodySmall, color = Muted,
        )
        Spacer(Modifier.height(40.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PermissionItem(r: PermRow) {
    val bg by animateColorAsState(if (r.granted) Ink else Snow, tween(300), label = "pbg")
    val fg = if (r.granted) Snow else Ink
    Row(
        Modifier
            .pressable(enabled = !r.granted && r.enabled) { r.ask() }
            .fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(bg)
            .graphicsLayer { alpha = if (r.enabled || r.granted) 1f else 0.45f }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(if (r.granted) Lime else Mist),
            contentAlignment = Alignment.Center,
        ) { Icon(r.icon, null, tint = Ink, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, style = MaterialTheme.typography.titleMedium, color = fg)
            Text(r.body, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.55f))
        }
        if (r.granted) Icon(Icons.Rounded.Check, null, tint = Lime)
        else Text("Allow", style = MaterialTheme.typography.labelLarge, color = Ink)
    }
}
