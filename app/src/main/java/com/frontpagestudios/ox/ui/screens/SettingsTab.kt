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
import com.frontpagestudios.ox.data.Places
import com.frontpagestudios.ox.ui.components.SavePlaceDialog
import com.frontpagestudios.ox.ui.components.icon
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TableChart
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.mutableStateOf
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.tracking.AutoDetect
import com.frontpagestudios.ox.ui.components.ModeSelector
import com.frontpagestudios.ox.ui.components.OxLogo
import androidx.compose.ui.unit.sp
import com.frontpagestudios.ox.ui.components.SectionLabel
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Perms

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    val places by Places.places.collectAsState()
    var addingPlace by remember { mutableStateOf(false) }
    val accent by Prefs.accent.collectAsState()
    val reminderOn by Prefs.reminder.collectAsState()
    val reminderMin by Prefs.reminderMinute.collectAsState()
    val trips by com.frontpagestudios.ox.data.TripRepo.trips.collectAsState()
    val io = androidx.compose.runtime.rememberCoroutineScope()

    fun writeTo(uri: android.net.Uri?, text: () -> String, done: String) {
        if (uri == null) return
        io.launch {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(text().toByteArray()) }; true }.getOrDefault(false)
            }
            Toast.makeText(ctx, if (ok) done else "Couldn't save the file", Toast.LENGTH_SHORT).show()
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        writeTo(uri, { com.frontpagestudios.ox.util.Export.csv(trips) }, "Exported ${trips.size} trips")
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        writeTo(uri, { com.frontpagestudios.ox.data.TripRepo.exportJson() }, "Backup saved")
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) io.launch {
            val n = runCatching {
                val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: ""
                }
                com.frontpagestudios.ox.data.TripRepo.importJson(text)
            }.getOrNull()
            Toast.makeText(ctx, if (n == null) "That file isn't an OX backup" else "Restored $n trips", Toast.LENGTH_SHORT).show()
        }
    }

    if (addingPlace) {
        SavePlaceDialog(title = "Save this spot", onDismiss = { addingPlace = false }) { name, kind ->
            addingPlace = false
            if (!Perms.location(ctx)) {
                Toast.makeText(ctx, "Allow location first", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, "Getting your location…", Toast.LENGTH_SHORT).show()
                currentLocation(ctx) { lat, lon ->
                    if (lat == null || lon == null) Toast.makeText(ctx, "Couldn't get a GPS fix. Try outdoors.", Toast.LENGTH_LONG).show()
                    else { Places.add(name, kind, lat, lon); Toast.makeText(ctx, "$name saved", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

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
        SectionLabel("App color")
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(16.dp)) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                com.frontpagestudios.ox.ui.theme.OxAccent.themes.forEach { (label, argb) ->
                    val on = accent == argb
                    Box(
                        Modifier.pressable {
                            Prefs.setAccent(argb)
                            com.frontpagestudios.ox.ui.theme.OxAccent.color = androidx.compose.ui.graphics.Color(argb)
                            com.frontpagestudios.ox.data.TripRepo.changed()
                        }.size(52.dp).clip(CircleShape).background(if (on) Ink else Mist),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(if (on) 34.dp else 40.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(argb)))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                com.frontpagestudios.ox.ui.theme.OxAccent.themes.firstOrNull { it.second == accent }?.first ?: "Custom",
                style = MaterialTheme.typography.titleMedium, color = Ink,
            )
            Text("Changes the whole app, widget and notifications.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Your places")
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(8.dp)) {
            places.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                        Icon(p.kind.icon(), null, tint = Lime, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(p.kind.label, style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Box(
                        Modifier.pressable { Places.remove(p.id) }.size(40.dp).clip(CircleShape).background(Mist),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Close, "Remove ${p.name}", tint = Ink, modifier = Modifier.size(18.dp)) }
                }
            }
            Row(
                Modifier.pressable { addingPlace = true }.fillMaxWidth().clip(RoundedCornerShape(24.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Lime), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, tint = Ink, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Add where I am now", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text("Stand at Home or Office and tap — or tap O / X on any trip", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
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
        SectionLabel("Leave-time reminder")
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(8.dp)) {
            val plan = remember(reminderOn, reminderMin, trips) { com.frontpagestudios.ox.tracking.Reminder.plan(trips) }
            ToggleRow(
                Icons.Rounded.NotificationsActive, "Remind me when to leave",
                if (plan == null) "Needs 2+ trips on the same route first" else "Mon–Sat, before your usual trip",
                reminderOn,
            ) { on ->
                Prefs.setReminder(on)
                com.frontpagestudios.ox.tracking.Reminder.schedule(ctx, trips)
                if (on && plan == null) Toast.makeText(ctx, "It will start once OX learns your usual route", Toast.LENGTH_LONG).show()
            }
            if (reminderOn && plan != null) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(com.frontpagestudios.ox.tracking.Reminder.label(plan.minute), style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text(if (reminderMin < 0) "Auto · 10 min before you usually leave" else "Set by you", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Box(
                        Modifier.pressable {
                            Prefs.setReminderMinute(((plan.minute - 5) + 1440) % 1440)
                            com.frontpagestudios.ox.tracking.Reminder.schedule(ctx, trips)
                        }.size(44.dp).clip(CircleShape).background(Mist),
                        contentAlignment = Alignment.Center,
                    ) { Text("−5", style = MaterialTheme.typography.labelLarge, color = Ink) }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.pressable {
                            Prefs.setReminderMinute((plan.minute + 5) % 1440)
                            com.frontpagestudios.ox.tracking.Reminder.schedule(ctx, trips)
                        }.size(44.dp).clip(CircleShape).background(Mist),
                        contentAlignment = Alignment.Center,
                    ) { Text("+5", style = MaterialTheme.typography.labelLarge, color = Ink) }
                    if (reminderMin >= 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.pressable {
                                Prefs.setReminderMinute(-1)
                                com.frontpagestudios.ox.tracking.Reminder.schedule(ctx, trips)
                            }.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Lime).padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Auto", style = MaterialTheme.typography.labelLarge, color = Ink) }
                    }
                }
                Text(plan.body, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp))
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Fuel cost (optional)")
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Add today's petrol price and your mileage — OX estimates the fuel each trip used.",
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
            NumberRow("Petrol price", "₹ / litre", Prefs.fuelPrice.collectAsState().value) { Prefs.setFuelPrice(it) }
            NumberRow("Bike mileage", "km / litre", Prefs.bikeKmpl.collectAsState().value) { Prefs.setBikeKmpl(it) }
            NumberRow("Car mileage", "km / litre", Prefs.carKmpl.collectAsState().value) { Prefs.setCarKmpl(it) }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Feel")
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(8.dp)) {
            ToggleRow(Icons.AutoMirrored.Rounded.VolumeUp, "Sound effects", "Taps, trip start/finish, every-km ping", sound) { Prefs.setSound(it) }
            ToggleRow(Icons.Rounded.Vibration, "Haptics", "Subtle vibration on taps", haptics) { Prefs.setHaptics(it) }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Your data")
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Snow).padding(8.dp)) {
            DataRow(Icons.Rounded.TableChart, "Export to Excel (CSV)", "Every trip with time, km, stops, fuel") {
                csvLauncher.launch("OX-trips.csv")
            }
            DataRow(Icons.Rounded.CloudUpload, "Back up trips", "Save a file — pick Google Drive to keep it safe") {
                backupLauncher.launch("OX-backup.json")
            }
            DataRow(Icons.Rounded.CloudDownload, "Restore from backup", "Bring trips back on a new phone") {
                restoreLauncher.launch(arrayOf("application/json", "*/*"))
            }
        }

        Spacer(Modifier.height(22.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Ink).padding(24.dp)
        ) {
            OxLogo(40.dp, Lime)
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

@android.annotation.SuppressLint("MissingPermission")
private fun currentLocation(ctx: android.content.Context, done: (Double?, Double?) -> Unit) {
    runCatching {
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(ctx)
            .getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc -> done(loc?.latitude, loc?.longitude) }
            .addOnFailureListener { done(null, null) }
    }.onFailure { done(null, null) }
}

@Composable
private fun NumberRow(label: String, unit: String, value: Float, onChange: (Float) -> Unit) {
    var text by remember { mutableStateOf(if (value > 0f) (if (value % 1f == 0f) value.toInt().toString() else value.toString()) else "") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Box(Modifier.width(110.dp).clip(RoundedCornerShape(18.dp)).background(Mist).padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (text.isEmpty()) Text("0", style = MaterialTheme.typography.titleMedium, color = Muted)
            BasicTextField(
                value = text,
                onValueChange = { v ->
                    val clean = v.filter { it.isDigit() || it == '.' }.take(6)
                    text = clean
                    onChange(clean.toFloatOrNull() ?: 0f)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = Ink),
                cursorBrush = SolidColor(Ink),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DataRow(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier.pressable(onClick = onClick).fillMaxWidth().clip(RoundedCornerShape(24.dp)).padding(12.dp),
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
    }
}
