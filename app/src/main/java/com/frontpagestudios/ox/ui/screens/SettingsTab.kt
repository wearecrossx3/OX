package com.frontpagestudios.ox.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.frontpagestudios.ox.BuildConfig
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.tracking.AutoDetect
import com.frontpagestudios.ox.ui.components.ModeSelector
import com.frontpagestudios.ox.ui.components.OxMark
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Perms

@Composable
fun SettingsTab() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    @Suppress("UNUSED_VARIABLE") val t = tick

    val name by Prefs.name.collectAsState()
    val auto by Prefs.autoDetect.collectAsState()
    val vehicle by Prefs.vehicle.collectAsState()
    val sound by Prefs.sound.collectAsState()
    val haptics by Prefs.haptics.collectAsState()
    val canAuto = Perms.canAuto(ctx)

    fun openAppSettings() {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Your\nsettings", style = MaterialTheme.typography.displayLarge, color = Ink)
        Spacer(Modifier.height(24.dp))

        SectionLabel("Name")
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Snow).padding(horizontal = 22.dp, vertical = 18.dp)) {
            if (name.isEmpty()) Text("Your name", style = MaterialTheme.typography.titleLarge, color = Muted)
            BasicTextField(
                value = name, onValueChange = { Prefs.setName(it.take(20)) }, singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(color = Ink), cursorBrush = SolidColor(Ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Tracking")
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(8.dp)
        ) {
            ToggleRow(
                Icons.Rounded.AutoAwesome, "Auto-detect trips",
                if (canAuto) "Starts when you ride or drive, stops when you park" else "Needs location “all the time” + physical activity",
                auto && canAuto,
            ) { on ->
                if (on && !canAuto) {
                    openAppSettings()
                } else {
                    Prefs.setAutoDetect(on)
                    if (on) AutoDetect.enable(ctx) else AutoDetect.disable(ctx)
                }
            }
            Spacer(Modifier.height(4.dp))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Your vehicle", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text("Used for auto-detected trips", style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(10.dp))
                ModeSelector(vehicle, { Prefs.setVehicle(it) }, modes = listOf(TripMode.BIKE, TripMode.CAR))
            }
        }
        if (!canAuto) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.pressable { openAppSettings() }.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Ink).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Lime)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Fix permissions", style = MaterialTheme.typography.titleMedium, color = Snow)
                    Text(
                        "Permissions → Location → Allow all the time, and Physical activity → Allow",
                        style = MaterialTheme.typography.bodySmall, color = Snow.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Feel")
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(8.dp)) {
            ToggleRow(Icons.AutoMirrored.Rounded.VolumeUp, "Sound effects", "Taps, trip start/finish, every-km ping", sound) { Prefs.setSound(it) }
            ToggleRow(Icons.Rounded.Vibration, "Haptics", "Subtle vibration on taps", haptics) { Prefs.setHaptics(it) }
        }

        Spacer(Modifier.height(22.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Ink).padding(24.dp)
        ) {
            OxMark(Modifier.width(150.dp).height(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("OX · origin to destination", style = MaterialTheme.typography.titleMedium, color = Snow)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = Snow.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, null, tint = Lime, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Trips stay on your phone. Maps © OpenStreetMap contributors © CARTO.",
                    style = MaterialTheme.typography.bodySmall, color = Snow.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Made by Frontpage Studios", style = MaterialTheme.typography.labelLarge, color = Lime)
        }
        Spacer(Modifier.height(150.dp))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, body: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.pressable { onChange(!on) }.fillMaxWidth().clip(RoundedCornerShape(24.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Mist), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.width(10.dp))
        OxSwitch(on)
    }
}

@Composable
fun OxSwitch(on: Boolean) {
    val track by animateColorAsState(if (on) Ink else Mist, tween(220), label = "tr")
    val knob by animateColorAsState(if (on) Lime else Snow, tween(220), label = "kn")
    val x by animateDpAsState(if (on) 24.dp else 0.dp, spring(dampingRatio = 0.55f, stiffness = 500f), label = "x")
    Box(Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(50)).background(track).padding(4.dp)) {
        Box(Modifier.offset(x = x).size(24.dp).clip(CircleShape).background(knob))
    }
}
