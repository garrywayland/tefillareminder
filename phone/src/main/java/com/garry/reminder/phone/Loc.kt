package com.garry.reminder.phone

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.garry.reminder.isIsraelHere
import com.garry.reminder.setLocation

/** Device location for prayer times. Rough (coarse) location only; saved on the device and never sent anywhere. */
object Loc {
    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Loads the saved location into the calendar rules. Cheap; call at every entry point. */
    fun apply(ctx: Context) {
        val p = ctx.getSharedPreferences("p", 0)
        if (p.contains("lat") && p.contains("lon")) setLocation(p.getFloat("lat", 0f).toDouble(), p.getFloat("lon", 0f).toDouble())
    }

    fun saved(ctx: Context): Pair<Double, Double>? {
        val p = ctx.getSharedPreferences("p", 0)
        return if (p.contains("lat") && p.contains("lon")) Pair(p.getFloat("lat", 0f).toDouble(), p.getFloat("lon", 0f).toDouble()) else null
    }

    /** Looks up the current location and saves it. [done] runs on the main thread only if a location was found. */
    @SuppressLint("MissingPermission")
    fun refresh(ctx: Context, done: () -> Unit = {}) {
        if (!hasPermission(ctx)) return
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return
        val providers = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (last != null) { save(ctx, last); done(); return }
        val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return
        if (Build.VERSION.SDK_INT >= 30) {
            lm.getCurrentLocation(provider, null, ctx.mainExecutor) { l -> if (l != null) { save(ctx, l); done() } }
        }
    }

    private fun save(ctx: Context, l: Location) {
        val p = ctx.getSharedPreferences("p", 0)
        val e = p.edit().putFloat("lat", l.latitude.toFloat()).putFloat("lon", l.longitude.toFloat())
        // Follow the device into/out of Israel, but only when that changes, so a manual choice sticks until you travel.
        val il = isIsraelHere()
        if (il != p.getBoolean("ilDetected", false)) e.putBoolean("il", il).putBoolean("ilDetected", il)
        e.apply()
        apply(ctx)
    }
}
