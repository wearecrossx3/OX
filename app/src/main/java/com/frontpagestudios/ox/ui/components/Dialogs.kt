package com.frontpagestudios.ox.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.frontpagestudios.ox.data.PlaceKind
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Txt
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Sfx

fun PlaceKind.icon(): ImageVector = when (this) {
    PlaceKind.HOME -> Icons.Rounded.Home
    PlaceKind.WORK -> Icons.Rounded.Work
    PlaceKind.GYM -> Icons.Rounded.FitnessCenter
    PlaceKind.SCHOOL -> Icons.Rounded.School
    PlaceKind.STAR -> Icons.Rounded.Star
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Mist).padding(horizontal = 18.dp, vertical = 14.dp)) {
        if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.titleMedium, color = Muted)
        BasicTextField(
            value = value, onValueChange = { onChange(it.take(28)) }, singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = Txt), cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Name a spot (Home, Office…) so trips there get that name automatically. */
@Composable
fun SavePlaceDialog(
    title: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (String, PlaceKind) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var kind by remember { mutableStateOf(PlaceKind.HOME) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Snow,
        shape = RoundedCornerShape(32.dp),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlaceKind.entries) { k ->
                        val on = k == kind
                        val bg by animateColorAsState(if (on) Ink else Mist, tween(200), label = "k")
                        Row(
                            Modifier.pressable {
                                val wasDefault = name.isBlank() || PlaceKind.entries.any { it.label == name }
                                kind = k
                                if (wasDefault) name = k.label
                            }.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(k.icon(), null, tint = if (on) Lime else Txt, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(k.label, style = MaterialTheme.typography.labelLarge, color = if (on) Lime else Txt)
                        }
                    }
                }
                Field(name, { name = it }, "Name, e.g. Office")
                Text(
                    "Trips starting or ending within ~180 m of here will use this name.",
                    style = MaterialTheme.typography.bodySmall, color = Muted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Sfx.play(Sfx.S.SUCCESS)
                onSave(name.ifBlank { kind.label }, kind)
            }) { Text("Save place", color = Txt, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted, style = MaterialTheme.typography.labelLarge) }
        },
    )
}

/** Rename both ends of one trip. */
@Composable
fun RenameTripDialog(from: String, to: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var a by remember { mutableStateOf(from) }
    var b by remember { mutableStateOf(to) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Snow,
        shape = RoundedCornerShape(32.dp),
        title = { Text("Rename this trip", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Origin (O)")
                Field(a, { a = it }, "Start")
                SectionLabel("Destination (X)")
                Field(b, { b = it }, "Finish")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(a.trim().ifEmpty { from }, b.trim().ifEmpty { to }) }) {
                Text("Save", color = Txt, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted, style = MaterialTheme.typography.labelLarge) }
        },
    )
}

/** Tapped an O or X on a trip: save it as a place, or rename the trip. */
@Composable
fun EndActionsDialog(label: String, name: String, onDismiss: () -> Unit, onSavePlace: () -> Unit, onRename: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Snow,
        shape = RoundedCornerShape(32.dp),
        title = { Text(name, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(label)
                ActionRow(Icons.Rounded.PushPin, "Save as a place", "Name it Home, Office… for every future trip", onSavePlace)
                ActionRow(Icons.Rounded.Edit, "Rename this trip", "Change the names on this trip only", onRename)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Muted, style = MaterialTheme.typography.labelLarge) }
        },
    )
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier.pressable(onClick = onClick).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Mist).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Lime, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Txt)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

/** Tiny trend line of trip durations, oldest → newest. */
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Lime) {
    androidx.compose.foundation.Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val mn = values.min(); val mx = values.max()
        val span = (mx - mn).coerceAtLeast(1f)
        val path = androidx.compose.ui.graphics.Path()
        values.forEachIndexed { i, v ->
            val x = i / (values.size - 1f) * size.width
            val y = size.height - (v - mn) / span * size.height * 0.8f - size.height * 0.1f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path, color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
        val last = values.last()
        drawCircle(color, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width, size.height - (last - mn) / span * size.height * 0.8f - size.height * 0.1f))
    }
}
