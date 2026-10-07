package com.frontpagestudios.ox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.frontpagestudios.ox.MainActivity
import com.frontpagestudios.ox.R
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.Trip
import com.frontpagestudios.ox.util.Format

/** Home-screen widget: last trip, weekly average, and a Start button. */
class OxWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Prefs.init(context)
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        /** Called whenever trips change. Stores a small summary the widget can draw without loading trips. */
        fun refresh(ctx: Context, trips: List<Trip>) {
            Prefs.init(ctx)
            val last = trips.firstOrNull()
            val weekAgo = System.currentTimeMillis() - 7L * 86_400_000
            val week = trips.filter { it.start > weekAgo && it.mode.isVehicle }
            val avg = if (week.isEmpty()) 0L else week.sumOf { it.durationMs } / week.size
            Prefs.raw().edit()
                .putString("w_route", last?.title ?: "No trips yet")
                .putString("w_time", last?.let { Format.duration(it.durationMs) } ?: "—")
                .putString("w_avg", if (week.isEmpty()) "Tap Start to record" else "7-day avg ${Format.duration(avg)} · ${week.size} trips")
                .putString("w_label", last?.let { Format.day(it.start) } ?: "OX")
                .apply()
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, OxWidget::class.java))
            ids.forEach { mgr.updateAppWidget(it, views(ctx)) }
        }

        private fun views(ctx: Context): RemoteViews {
            val sp = Prefs.raw()
            val accent = Prefs.accent.value.toInt()
            return RemoteViews(ctx.packageName, R.layout.widget_ox).apply {
                setTextViewText(R.id.w_route, sp.getString("w_route", "No trips yet"))
                setTextViewText(R.id.w_time, sp.getString("w_time", "—"))
                setTextColor(R.id.w_time, accent)
                setTextViewText(R.id.w_avg, sp.getString("w_avg", "Tap Start to record"))
                setTextViewText(R.id.w_label, sp.getString("w_label", "Last trip"))
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    setColorStateList(R.id.w_start, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(accent))
                }
                val start = PendingIntent.getActivity(
                    ctx, 21,
                    Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_START_TRIP, true)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                setOnClickPendingIntent(R.id.w_start, start)
                val open = PendingIntent.getActivity(
                    ctx, 22, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }
    }
}
