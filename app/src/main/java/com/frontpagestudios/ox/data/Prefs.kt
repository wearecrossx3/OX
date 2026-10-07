package com.frontpagestudios.ox.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Prefs {
    private lateinit var sp: SharedPreferences

    private val _onboarded = MutableStateFlow(false)
    val onboarded: StateFlow<Boolean> = _onboarded
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name
    private val _autoDetect = MutableStateFlow(true)
    val autoDetect: StateFlow<Boolean> = _autoDetect
    private val _vehicle = MutableStateFlow(TripMode.BIKE)
    val vehicle: StateFlow<TripMode> = _vehicle
    private val _sound = MutableStateFlow(true)
    val sound: StateFlow<Boolean> = _sound
    private val _haptics = MutableStateFlow(true)
    val haptics: StateFlow<Boolean> = _haptics
    private val _bikeKmpl = MutableStateFlow(45f)
    val bikeKmpl: StateFlow<Float> = _bikeKmpl
    private val _carKmpl = MutableStateFlow(15f)
    val carKmpl: StateFlow<Float> = _carKmpl
    private val _fuelPrice = MutableStateFlow(0f)
    val fuelPrice: StateFlow<Float> = _fuelPrice

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("ox_prefs", Context.MODE_PRIVATE)
        _onboarded.value = sp.getBoolean("onboarded", false)
        _name.value = sp.getString("name", "") ?: ""
        _autoDetect.value = sp.getBoolean("autoDetect", true)
        _vehicle.value = runCatching { TripMode.valueOf(sp.getString("vehicle", "BIKE")!!) }.getOrDefault(TripMode.BIKE)
        _sound.value = sp.getBoolean("sound", true)
        _haptics.value = sp.getBoolean("haptics", true)
        _bikeKmpl.value = sp.getFloat("bikeKmpl", 45f)
        _carKmpl.value = sp.getFloat("carKmpl", 15f)
        _fuelPrice.value = sp.getFloat("fuelPrice", 0f)
    }

    fun setOnboarded(v: Boolean) { _onboarded.value = v; sp.edit().putBoolean("onboarded", v).apply() }
    fun setName(v: String) { _name.value = v; sp.edit().putString("name", v).apply() }
    fun setAutoDetect(v: Boolean) { _autoDetect.value = v; sp.edit().putBoolean("autoDetect", v).apply() }
    fun setVehicle(v: TripMode) { _vehicle.value = v; sp.edit().putString("vehicle", v.name).apply() }
    fun setSound(v: Boolean) { _sound.value = v; sp.edit().putBoolean("sound", v).apply() }
    fun setBikeKmpl(v: Float) { _bikeKmpl.value = v; sp.edit().putFloat("bikeKmpl", v).apply() }
    fun setCarKmpl(v: Float) { _carKmpl.value = v; sp.edit().putFloat("carKmpl", v).apply() }
    fun setFuelPrice(v: Float) { _fuelPrice.value = v; sp.edit().putFloat("fuelPrice", v).apply() }

    /** Fuel cost in rupees for a distance, or null when no price is set / walking. */
    fun fuelCost(mode: TripMode, meters: Double): Double? {
        val price = _fuelPrice.value
        if (price <= 0f || mode == TripMode.WALK) return null
        val kmpl = if (mode == TripMode.CAR) _carKmpl.value else _bikeKmpl.value
        if (kmpl <= 0f) return null
        return meters / 1000.0 / kmpl * price
    }

    fun setHaptics(v: Boolean) { _haptics.value = v; sp.edit().putBoolean("haptics", v).apply() }
}
