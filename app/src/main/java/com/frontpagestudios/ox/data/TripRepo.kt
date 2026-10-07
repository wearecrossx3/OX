package com.frontpagestudios.ox.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Stores every trip as a small JSON file on the phone. No server, no account. */
object TripRepo {
    private lateinit var dir: File
    private lateinit var liveFile: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun init(context: Context) {
        if (::dir.isInitialized) return
        dir = File(context.filesDir, "trips").apply { mkdirs() }
        liveFile = File(context.filesDir, "live.json")
        scope.launch {
            val list = dir.listFiles { f -> f.name.endsWith(".json") }
                ?.mapNotNull { f -> runCatching { tripFromJson(JSONObject(f.readText())) }.getOrNull() }
                ?.sortedByDescending { it.start }
                ?: emptyList()
            _trips.value = list
            _loaded.value = true
        }
    }

    fun get(id: String): Trip? = _trips.value.firstOrNull { it.id == id }

    suspend fun save(trip: Trip) = withContext(Dispatchers.IO) {
        File(dir, "${trip.id}.json").writeText(tripToJson(trip).toString())
        _trips.update { list -> (listOf(trip) + list.filter { it.id != trip.id }).sortedByDescending { it.start } }
    }

    fun delete(id: String) {
        _trips.update { list -> list.filter { it.id != id } }
        scope.launch { File(dir, "$id.json").delete() }
    }

    // ---- live snapshot, so a trip survives if Android restarts the service ----

    fun saveLiveAsync(live: LiveTrip) {
        scope.launch { runCatching { liveFile.writeText(liveToJson(live).toString()) } }
    }

    fun loadLive(): LiveTrip? = runCatching {
        if (!liveFile.exists()) null else liveFromJson(JSONObject(liveFile.readText()))
    }.getOrNull()

    fun clearLive() {
        runCatching { liveFile.delete() }
    }

    // ---- json ----

    private fun pointsToJson(points: List<TripPoint>) = JSONArray().apply {
        points.forEach { p ->
            put(JSONArray().put(p.lat).put(p.lon).put(p.t).put(p.speed.toDouble()))
        }
    }

    private fun pointsFromJson(arr: JSONArray): List<TripPoint> = List(arr.length()) { i ->
        val a = arr.getJSONArray(i)
        TripPoint(a.getDouble(0), a.getDouble(1), a.getLong(2), a.getDouble(3).toFloat())
    }

    private fun tripToJson(t: Trip) = JSONObject().apply {
        put("id", t.id); put("start", t.start); put("end", t.end)
        put("mode", t.mode.name); put("auto", t.auto)
        put("distance", t.distance); put("maxSpeed", t.maxSpeed.toDouble())
        put("origin", t.origin ?: JSONObject.NULL); put("destination", t.destination ?: JSONObject.NULL)
        put("points", pointsToJson(t.points))
    }

    private fun tripFromJson(o: JSONObject) = Trip(
        id = o.getString("id"),
        start = o.getLong("start"),
        end = o.getLong("end"),
        mode = runCatching { TripMode.valueOf(o.getString("mode")) }.getOrDefault(TripMode.BIKE),
        auto = o.optBoolean("auto"),
        points = pointsFromJson(o.getJSONArray("points")),
        distance = o.getDouble("distance"),
        maxSpeed = o.optDouble("maxSpeed", 0.0).toFloat(),
        origin = if (o.isNull("origin")) null else o.optString("origin"),
        destination = if (o.isNull("destination")) null else o.optString("destination"),
    )

    private fun liveToJson(l: LiveTrip) = JSONObject().apply {
        put("start", l.start); put("mode", l.mode.name); put("auto", l.auto)
        put("distance", l.distance); put("maxSpeed", l.maxSpeed.toDouble()); put("lastMoving", l.lastMoving)
        put("points", pointsToJson(l.points))
    }

    private fun liveFromJson(o: JSONObject): LiveTrip {
        val pts = pointsFromJson(o.getJSONArray("points"))
        return LiveTrip(
            start = o.getLong("start"),
            mode = runCatching { TripMode.valueOf(o.getString("mode")) }.getOrDefault(TripMode.BIKE),
            auto = o.optBoolean("auto"),
            points = pts,
            distance = o.optDouble("distance", 0.0),
            maxSpeed = o.optDouble("maxSpeed", 0.0).toFloat(),
            lastMoving = o.optLong("lastMoving", System.currentTimeMillis()),
            current = pts.lastOrNull(),
        )
    }
}
