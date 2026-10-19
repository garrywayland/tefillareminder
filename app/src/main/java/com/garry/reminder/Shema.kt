package com.garry.reminder

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

/** Optional Shema display on the watch: pref "shema" 0 = off, 1 = show on complication/tile until you open it. */
object Shema {
    fun on(ctx: Context) = ctx.getSharedPreferences("p", 0).getInt("shema", 0) > 0
    fun ack(ctx: Context) = ctx.getSharedPreferences("p", 0).getString("shemaAck", "") ?: ""

    fun refreshSurfaces(ctx: Context) {
        runCatching { ComplicationDataSourceUpdateRequester.create(ctx, ComponentName(ctx, SiddurComplicationService::class.java)).requestUpdateAll() }
        runCatching { TileService.getUpdater(ctx).requestUpdate(DavenTileService::class.java) }
    }
}
