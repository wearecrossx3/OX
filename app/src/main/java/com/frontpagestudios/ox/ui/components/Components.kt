package com.frontpagestudios.ox.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.ui.theme.Graphite
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Muted
import com.frontpagestudios.ox.ui.theme.Snow
import com.frontpagestudios.ox.util.Sfx
import kotlinx.coroutines.delay

/** Springy press feedback + tap sound + haptic. Used on every tappable surface. */
fun Modifier.pressable(
    enabled: Boolean = true,
    sound: Sfx.S? = Sfx.S.TAP,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press",
    )
    val haptic = LocalHapticFeedback.current
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            sound?.let { Sfx.play(it) }
            if (Prefs.haptics.value) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

fun TripMode.icon(): ImageVector = when (this) {
    TripMode.CAR -> Icons.Rounded.DirectionsCar
    TripMode.BIKE -> Icons.Rounded.TwoWheeler
    TripMode.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
}

/** Big lime call-to-action, like "Book a ride" in the reference. */
@Composable
fun LimePill(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    container: Color = Lime,
    content: Color = Ink,
    icon: ImageVector = Icons.Rounded.NorthEast,
    sound: Sfx.S? = Sfx.S.TAP,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .pressable(sound = sound, onClick = onClick)
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(42.dp))
            .background(container)
            .padding(start = 28.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = content)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.6f))
            }
        }
        Box(
            Modifier.size(60.dp).clip(CircleShape).background(content),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = container, modifier = Modifier.size(26.dp))
        }
    }
}

/** Rounded stat tile. dark = black tile with lime value (accent tile). */
@Composable
fun StatTile(
    value: String,
    unit: String?,
    label: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    lime: Boolean = false,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val bg = when { lime -> Lime; dark -> Ink; else -> Mist }
    val fg = when { lime -> Ink; dark -> Snow; else -> Ink }
    val valueColor = if (dark && !lime) Lime else fg
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .clip(RoundedCornerShape(32.dp))
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (icon != null) {
                Icon(icon, null, tint = valueColor, modifier = Modifier.size(22.dp).padding(bottom = 4.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineLarge, color = valueColor)
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    unit, style = MaterialTheme.typography.labelLarge,
                    color = valueColor.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.55f))
    }
}

@Composable
fun ModeSelector(
    selected: TripMode,
    onSelect: (TripMode) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    modes: List<TripMode> = TripMode.entries,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { m ->
            val on = m == selected
            val bg by androidx.compose.animation.animateColorAsState(
                when { on -> Lime; dark -> Graphite; else -> Mist }, tween(250), label = "modeBg",
            )
            val fg = when { on -> Ink; dark -> Snow; else -> Ink }
            Row(
                Modifier
                    .weight(1f)
                    .pressable { onSelect(m) }
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(bg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(m.icon(), null, tint = fg, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(m.label, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

@Composable
fun CircleButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    bg: Color = Snow,
    fg: Color = Ink,
    onClick: () -> Unit,
) {
    Box(
        modifier.pressable(onClick = onClick).size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
fun PulseDot(color: Color = Lime, size: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, 2.4f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "s")
    val a by t.animateFloat(0.6f, 0f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "a")
    Box(Modifier.size(size * 2.4f), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).scale(s).alpha(a).clip(CircleShape).background(color))
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

/** Number that rolls to its new value. */
@Composable
fun AnimatedNumber(
    value: Float,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    durationMs: Int = 900,
) {
    val v by animateFloatAsState(value, tween(durationMs, easing = FastOutSlowInEasing), label = "num")
    val txt = if (decimals == 0) v.toInt().toString() else String.format(java.util.Locale.US, "%.${decimals}f", v)
    Text(txt, style = style, color = color, modifier = modifier)
}

/** Fades + lifts its content in after [delayMs]. Used for staggered screen entrances. */
@Composable
fun Reveal(delayMs: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(520, easing = FastOutSlowInEasing), label = "ra")
    val y by animateFloatAsState(if (shown) 0f else 46f, spring(dampingRatio = 0.7f, stiffness = 220f), label = "ry")
    Box(modifier.graphicsLayer { alpha = a; translationY = y }) { content() }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, bg: Color = Snow, fg: Color = Ink, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}
