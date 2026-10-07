package com.frontpagestudios.ox.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Perms {
    private fun has(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun location(ctx: Context) = has(ctx, Manifest.permission.ACCESS_FINE_LOCATION)

    fun background(ctx: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || has(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    fun activity(ctx: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || has(ctx, Manifest.permission.ACTIVITY_RECOGNITION)

    fun notifications(ctx: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || has(ctx, Manifest.permission.POST_NOTIFICATIONS)

    fun canAuto(ctx: Context) = location(ctx) && background(ctx) && activity(ctx)
}
