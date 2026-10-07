package com.frontpagestudios.ox.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.frontpagestudios.ox.data.TripPoint
import org.osmdroid.util.GeoPoint
import java.util.Locale

object Geo {
    fun dist(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val r = FloatArray(1)
        Location.distanceBetween(aLat, aLon, bLat, bLon, r)
        return r[0].toDouble()
    }

    fun length(points: List<TripPoint>): Double {
        var d = 0.0
        for (i in 1 until points.size) {
            d += dist(points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon)
        }
        return d
    }

    fun geo(points: List<TripPoint>): List<GeoPoint> = points.map { GeoPoint(it.lat, it.lon) }

    /** Short human name for a spot: area / street / city. Works when online; null otherwise. */
    @Suppress("DEPRECATION")
    fun place(context: Context, p: TripPoint): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            val list = Geocoder(context, Locale.getDefault()).getFromLocation(p.lat, p.lon, 1)
            val a = list?.firstOrNull()
            if (a == null) null else {
                val feature = a.featureName?.takeIf { f -> f.any { it.isLetter() } && f.length < 28 }
                a.subLocality ?: a.thoroughfare ?: feature ?: a.locality ?: a.subAdminArea
            }
        } catch (e: Exception) {
            null
        }
    }
}
