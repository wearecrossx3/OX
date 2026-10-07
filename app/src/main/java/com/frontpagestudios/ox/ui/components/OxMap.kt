package com.frontpagestudios.ox.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.frontpagestudios.ox.R
import com.frontpagestudios.ox.ui.theme.LimeArgb
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

private val CartoDark = XYTileSource(
    "CartoDarkMatter", 0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
    ),
    "© OpenStreetMap contributors © CARTO",
)

private val CartoLight = XYTileSource(
    "CartoPositron", 0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_all/",
        "https://b.basemaps.cartocdn.com/light_all/",
        "https://c.basemaps.cartocdn.com/light_all/",
    ),
    "© OpenStreetMap contributors © CARTO",
)

/**
 * Map with a lime route line, an "O" marker at the origin and an "X" at the destination.
 * Free OpenStreetMap / CARTO tiles, no API key.
 */
@Composable
fun OxMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    dark: Boolean = true,
    interactive: Boolean = true,
    follow: GeoPoint? = null,
    showEnds: Boolean = true,
    liveDot: GeoPoint? = null,
    fitKey: Any? = null,
    fitPoints: List<GeoPoint>? = null,
    recenterKey: Any? = null,
    lineWidthDp: Float = 5f,
    extraLines: List<List<GeoPoint>> = emptyList(),
    extraKey: Any? = null,
    stops: List<GeoPoint> = emptyList(),
) {
    val context = LocalContext.current
    val holder = remember { MapHolder(context, dark) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> holder.map.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.map.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        holder.map.onResume()
        onDispose {
            lifecycle.removeObserver(obs)
            holder.map.onPause()
            holder.map.onDetach()
        }
    }
    AndroidView(
        factory = { holder.map },
        modifier = modifier,
        update = { holder.setExtras(extraLines, extraKey); holder.setStops(stops); holder.update(points, interactive, follow, showEnds, liveDot, fitKey, fitPoints, recenterKey, lineWidthDp) },
    )
}

private class MapHolder(val ctx: Context, dark: Boolean) {
    private val density = ctx.resources.displayMetrics.density
    private val bgColor = if (dark) 0xFF141414.toInt() else 0xFFE9E9E4.toInt()

    val map: MapView = MapView(ctx).apply {
        setTileSource(if (dark) CartoDark else CartoLight)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        setTilesScaledToDpi(true)
        minZoomLevel = 3.0
        maxZoomLevel = 19.5
        setHorizontalMapRepetitionEnabled(false)
        setVerticalMapRepetitionEnabled(false)
        controller.setZoom(15.5)
        controller.setCenter(GeoPoint(22.3039, 70.8022))
        setBackgroundColor(bgColor)
        overlayManager.tilesOverlay.setLoadingBackgroundColor(bgColor)
        overlayManager.tilesOverlay.setLoadingLineColor(bgColor)
    }

    private val glow = Polyline(map).apply {
        outlinePaint.apply {
            color = (LimeArgb and 0x00FFFFFF) or (0x40 shl 24)
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
        }
        setInfoWindow(null as InfoWindow?)
    }
    private val line = Polyline(map).apply {
        outlinePaint.apply {
            color = LimeArgb
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
        }
        setInfoWindow(null as InfoWindow?)
    }
    private val startM = marker(pin("O", 0xFF0D0D0D.toInt(), LimeArgb))
    private val endM = marker(pin("X", LimeArgb, 0xFF0D0D0D.toInt()))
    private val dotM = marker(dot())

    private var lastFit: Any? = Any()
    private var lastRecenter: Any? = Any()
    private var lastCount = -1
    private var interactiveNow = true

    init {
        map.overlays.add(glow)
        map.overlays.add(line)
        map.overlays.add(startM)
        map.overlays.add(endM)
        map.overlays.add(dotM)
    }

    private val stopMarkers = mutableListOf<Marker>()
    private var lastStops: List<GeoPoint>? = null

    fun setStops(list: List<GeoPoint>) {
        if (list == lastStops) return
        lastStops = list
        stopMarkers.forEach { map.overlays.remove(it) }
        stopMarkers.clear()
        list.forEachIndexed { i, gp ->
            val m = marker(pin("${i + 1}", 0xFFFFB020.toInt(), 0xFF0D0D0D.toInt(), 24))
            m.position = gp
            m.isEnabled = true
            stopMarkers.add(m)
            map.overlays.add(m)
        }
        map.invalidate()
    }

    private val extras = mutableListOf<Polyline>()
    private var lastExtraKey: Any? = Any()

    fun setExtras(lines: List<List<GeoPoint>>, key: Any?) {
        if (key == lastExtraKey) return
        lastExtraKey = key
        extras.forEach { map.overlays.remove(it) }
        extras.clear()
        lines.forEachIndexed { i, pts ->
            val pl = Polyline(map).apply {
                outlinePaint.apply {
                    color = 0x55FFFFFF
                    strokeWidth = 3f * density
                    strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true
                }
                setInfoWindow(null as InfoWindow?)
                setPoints(pts)
            }
            extras.add(pl)
            map.overlays.add(i, pl)
        }
        map.invalidate()
    }

    private fun marker(icon: BitmapDrawable) = Marker(map).apply {
        this.icon = icon
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        position = GeoPoint(0.0, 0.0)
        isEnabled = false
        setInfoWindow(null as MarkerInfoWindow?)
        setOnMarkerClickListener { _, _ -> true }
    }

    fun update(
        points: List<GeoPoint>,
        interactive: Boolean,
        follow: GeoPoint?,
        showEnds: Boolean,
        liveDot: GeoPoint?,
        fitKey: Any?,
        fitPoints: List<GeoPoint>?,
        recenterKey: Any?,
        widthDp: Float,
    ) {
        if (interactive != interactiveNow) {
            interactiveNow = interactive
            map.setMultiTouchControls(interactive)
            map.isClickable = interactive
        }
        line.outlinePaint.strokeWidth = widthDp * density
        glow.outlinePaint.strokeWidth = widthDp * density * 3.2f
        if (points.size != lastCount) {
            lastCount = points.size
            line.setPoints(points)
            glow.setPoints(points)
        }
        if (showEnds && points.isNotEmpty()) {
            startM.position = points.first(); startM.isEnabled = true
            if (points.size > 1 && liveDot == null) { endM.position = points.last(); endM.isEnabled = true } else endM.isEnabled = false
        } else {
            startM.isEnabled = false; endM.isEnabled = false
        }
        if (liveDot != null) { dotM.position = liveDot; dotM.isEnabled = true } else dotM.isEnabled = false

        if (follow != null) {
            val recenter = recenterKey != lastRecenter
            lastRecenter = recenterKey
            if (recenter) {
                map.controller.setZoom(17.0)
                map.controller.setCenter(follow)
            } else {
                map.controller.animateTo(follow)
            }
        } else if (fitKey != lastFit) {
            lastFit = fitKey
            fit(fitPoints ?: points)
        }
        map.invalidate()
    }

    private fun fit(points: List<GeoPoint>) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            map.controller.setZoom(16.0); map.controller.setCenter(points.first()); return
        }
        val box = BoundingBox.fromGeoPointsSafe(points)
        val run = {
            map.zoomToBoundingBox(box, false, (56 * density).toInt())
            if (map.zoomLevelDouble > 17.5) map.controller.setZoom(17.5)
        }
        if (map.width > 0 && map.height > 0) run() else map.addOnFirstLayoutListener { _, _, _, _, _ -> run() }
    }

    private fun font(): Typeface =
        runCatching { ResourcesCompat.getFont(ctx, R.font.space_grotesk) }.getOrNull() ?: Typeface.DEFAULT_BOLD

    private fun pin(label: String, bg: Int, fg: Int, sizeDp: Int = 34): BitmapDrawable {
        val s = (sizeDp * density).toInt()
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = fg; c.drawCircle(s / 2f, s / 2f, s / 2f, p)
        p.color = bg; c.drawCircle(s / 2f, s / 2f, s / 2f - 2.5f * density, p)
        p.color = fg
        p.textSize = s * 0.5f
        p.typeface = Typeface.create(font(), Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        val y = s / 2f - (p.descent() + p.ascent()) / 2
        c.drawText(label, s / 2f, y, p)
        return BitmapDrawable(ctx.resources, bmp)
    }

    private fun dot(): BitmapDrawable {
        val s = (30 * density).toInt()
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = (LimeArgb and 0x00FFFFFF) or (0x55 shl 24); c.drawCircle(s / 2f, s / 2f, s / 2f, p)
        p.color = 0xFFFFFFFF.toInt(); c.drawCircle(s / 2f, s / 2f, s * 0.26f, p)
        p.color = LimeArgb; c.drawCircle(s / 2f, s / 2f, s * 0.19f, p)
        return BitmapDrawable(ctx.resources, bmp)
    }
}
