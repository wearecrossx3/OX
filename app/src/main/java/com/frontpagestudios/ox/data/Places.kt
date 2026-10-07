package com.frontpagestudios.ox.data

import android.content.Context
import android.content.SharedPreferences
import com.frontpagestudios.ox.util.Geo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class PlaceKind(val label: String) { HOME("Home"), WORK("Office"), GYM("Gym"), SCHOOL("School"), STAR("Place") }

data class Place(
    val id: String,
    val name: String,
    val kind: PlaceKind,
    val lat: Double,
    val lon: Double,
    val radius: Double = 180.0,
)

/** Places the user has named (Home, Office…). Trips that start or end near one get its name. */
object Places {
    private lateinit var sp: SharedPreferences
    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("ox_places", Context.MODE_PRIVATE)
        _places.value = runCatching {
            val arr = JSONArray(sp.getString("list", "[]"))
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Place(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    kind = runCatching { PlaceKind.valueOf(o.getString("kind")) }.getOrDefault(PlaceKind.STAR),
                    lat = o.getDouble("lat"),
                    lon = o.getDouble("lon"),
                    radius = o.optDouble("radius", 180.0),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<Place>) {
        _places.value = list
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().put("id", p.id).put("name", p.name).put("kind", p.kind.name)
                    .put("lat", p.lat).put("lon", p.lon).put("radius", p.radius)
            )
        }
        sp.edit().putString("list", arr.toString()).apply()
    }

    fun add(name: String, kind: PlaceKind, lat: Double, lon: Double) {
        // replace a place that sits on the same spot
        val rest = _places.value.filter { Geo.dist(it.lat, it.lon, lat, lon) > 60 }
        persist(rest + Place("p${System.currentTimeMillis()}", name.trim().ifEmpty { kind.label }, kind, lat, lon))
    }

    fun remove(id: String) = persist(_places.value.filter { it.id != id })

    fun match(lat: Double, lon: Double): Place? = _places.value
        .map { it to Geo.dist(it.lat, it.lon, lat, lon) }
        .filter { (p, d) -> d <= p.radius }
        .minByOrNull { it.second }?.first

    fun match(p: TripPoint?): Place? = p?.let { match(it.lat, it.lon) }
}
