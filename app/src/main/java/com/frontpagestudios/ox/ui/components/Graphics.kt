package com.frontpagestudios.ox.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frontpagestudios.ox.data.TripPoint
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Muted
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Draws only the first [progress] fraction of [path]. */
fun DrawScope.drawPartialPath(path: Path, progress: Float, color: Color, width: Float, glow: Boolean = false) {
    val pm = PathMeasure()
    pm.setPath(path, false)
    val seg = Path()
    pm.getSegment(0f, pm.length * progress.coerceIn(0f, 1f), seg, true)
    if (glow) drawPath(seg, color.copy(alpha = 0.18f), style = Stroke(width * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(seg, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Route "shape" thumbnail, no map tiles. Animates in. */
@Composable
fun RouteShape(
    points: List<TripPoint>,
    modifier: Modifier = Modifier,
    color: Color = Lime,
    stroke: Float = 5f,
    animate: Boolean = true,
) {
    val progress = remember(points.size) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(points.size) { progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val lat0 = points.minOf { it.lat }; val lat1 = points.maxOf { it.lat }
        val lon0 = points.minOf { it.lon }; val lon1 = points.maxOf { it.lon }
        val k = cos(Math.toRadians((lat0 + lat1) / 2)) // keep proportions
        val w = max((lon1 - lon0) * k, 1e-6); val h = max(lat1 - lat0, 1e-6)
        val pad = size.minDimension * 0.18f
        val scale = min((size.width - pad * 2) / w, (size.height - pad * 2) / h).toFloat()
        val ox = (size.width - (w * scale).toFloat()) / 2
        val oy = (size.height - (h * scale).toFloat()) / 2
        fun map(p: TripPoint) = Offset(ox + ((p.lon - lon0) * k * scale).toFloat(), oy + ((lat1 - p.lat) * scale).toFloat())
        val path = Path()
        val step = max(1, points.size / 300)
        points.forEachIndexed { i, p ->
            if (i % step != 0 && i != points.lastIndex) return@forEachIndexed
            val o = map(p)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPartialPath(path, progress.value, color, stroke, glow = true)
        val s = map(points.first())
        drawCircle(color, stroke * 1.3f, s, style = Stroke(stroke * 0.7f))
        if (progress.value > 0.98f) drawCircle(color, stroke * 1.1f, map(points.last()))
    }
}

/** Speed over time, lime line with soft fill. */
@Composable
fun SpeedGraph(points: List<TripPoint>, modifier: Modifier = Modifier, line: Color = Lime) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        if (points.size < 3) return@Canvas
        val t0 = points.first().t; val t1 = points.last().t
        val span = max(1L, t1 - t0).toFloat()
        // light smoothing
        val sp = points.map { it.speed }.windowed(5, 1, partialWindows = true) { it.average().toFloat() }
        val vmax = max(sp.maxOrNull() ?: 1f, 1f) * 1.15f
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = (p.t - t0) / span * size.width
            val y = size.height - sp[i] / vmax * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        val clipW = size.width * progress.value
        drawContext.canvas.save()
        drawContext.canvas.clipRect(0f, 0f, clipW, size.height)
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawContext.canvas.restore()
        // baseline dashes
        for (g in 1..3) {
            val y = size.height * g / 4f
            drawLine(
                Muted.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f)),
            )
        }
    }
}

/** Seven bars, Mon..Sun. values in any unit; highlight = index to draw in lime. */
@Composable
fun WeekBars(
    values: List<Float>,
    labels: List<String>,
    highlight: Int,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White.copy(alpha = 0.14f),
    labelColor: Color = Muted,
) {
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val vmax = max(values.maxOrNull() ?: 0f, 1f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val n = values.size
            val gap = size.width * 0.035f
            val bw = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val x = i * (bw + gap)
                // track
                drawRoundRect(barColor, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 2))
                val h = (v / vmax * size.height * progress.value).coerceAtLeast(if (v > 0) bw else 0f)
                if (h > 0f) {
                    drawRoundRect(
                        if (i == highlight) Lime else Color.White.copy(alpha = 0.85f),
                        Offset(x, size.height - h), Size(bw, h), CornerRadius(bw / 2),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(
                    l, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == highlight) Lime else labelColor,
                )
            }
        }
    }
}

/** The OX mark: an origin ring, a route that draws itself, and the destination cross. */
@Composable
fun OxMark(modifier: Modifier = Modifier, color: Color = Lime, loop: Boolean = true) {
    val t = rememberInfiniteTransition(label = "ox")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), if (loop) RepeatMode.Restart else RepeatMode.Restart),
        label = "p",
    )
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val sw = h * 0.1f
        val r = h * 0.22f
        val o = Offset(r + sw, h / 2)
        val xC = Offset(w - r - sw, h / 2)
        // ring
        drawCircle(color, r, o, style = Stroke(sw))
        // dashed route that travels
        val route = Path().apply {
            moveTo(o.x + r + sw, h / 2)
            cubicTo(w * 0.42f, h * 0.05f, w * 0.58f, h * 0.95f, xC.x - r - sw, h / 2)
        }
        val drawP = (p * 1.6f).coerceAtMost(1f)
        drawPartialPath(route, drawP, color.copy(alpha = 0.9f), sw * 0.45f)
        // moving dot
        val pm = PathMeasure(); pm.setPath(route, false)
        val pos = pm.getPosition(pm.length * drawP)
        drawCircle(color, sw * 0.6f, pos)
        // cross
        val a = if (drawP >= 1f) 1f else 0.25f
        drawLine(color.copy(alpha = a), Offset(xC.x - r, xC.y - r), Offset(xC.x + r, xC.y + r), sw, StrokeCap.Round)
        drawLine(color.copy(alpha = a), Offset(xC.x + r, xC.y - r), Offset(xC.x - r, xC.y + r), sw, StrokeCap.Round)
    }
}

/** Placeholder card art: a looping route drawn on a dark grid. */
@Composable
fun EmptyRouteArt(modifier: Modifier = Modifier, caption: String) {
    val t = rememberInfiniteTransition(label = "empty")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 34.dp.toPx()
            var x = 0f
            while (x < size.width) { drawLine(Color.White.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
            var y = 0f
            while (y < size.height) { drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
            val path = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.75f)
                lineTo(size.width * 0.35f, size.height * 0.75f)
                lineTo(size.width * 0.35f, size.height * 0.45f)
                cubicTo(size.width * 0.5f, size.height * 0.2f, size.width * 0.6f, size.height * 0.6f, size.width * 0.72f, size.height * 0.35f)
                lineTo(size.width * 0.85f, size.height * 0.35f)
            }
            drawPartialPath(path, p, Lime, 4.dp.toPx(), glow = true)
            drawCircle(Lime, 7.dp.toPx(), Offset(size.width * 0.15f, size.height * 0.75f), style = Stroke(3.dp.toPx()))
            drawCircle(Ink, 6.dp.toPx(), Offset(size.width * 0.15f, size.height * 0.75f))
        }
        Text(
            caption, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
        )
    }
}
