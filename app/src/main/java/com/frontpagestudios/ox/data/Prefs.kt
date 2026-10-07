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

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("ox_prefs", Context.MODE_PRIVATE)
        _onboarded.value = sp.getBoolean("onboarded", false)
        _name.value = sp.getString("name", "") ?: ""
        _autoDetect.value = sp.getBoolean("autoDetect", true)
        _vehicle.value = runCatching { TripMode.valueOf(sp.getString("vehicle", "BIKE")!!) }.getOrDefault(TripMode.BIKE)
        _sound.value = sp.getBoolean("sound", true)
        _haptics.value = sp.getBoolean("haptics", true)
    }

    fun setOnboarded(v: Boolean) { _onboarded.value = v; sp.edit().putBoolean("onboarded", v).apply() }
    fun setName(v: String) { _name.value = v; sp.edit().putString("name", v).apply() }
    fun setAutoDetect(v: Boolean) { _autoDetect.value = v; sp.edit().putBoolean("autoDetect", v).apply() }
    fun setVehicle(v: TripMode) { _vehicle.value = v; sp.edit().putString("vehicle", v.name).apply() }
    fun setSound(v: Boolean) { _sound.value = v; sp.edit().putBoolean("sound", v).apply() }
    fun setHaptics(v: Boolean) { _haptics.value = v; sp.edit().putBoolean("haptics", v).apply() }
}
