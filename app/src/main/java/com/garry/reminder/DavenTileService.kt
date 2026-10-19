package com.garry.reminder

import androidx.wear.protolayout.*
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.time.LocalDate

/** Swipe-in tile: what is due now, and what is due at the next service. */
class DavenTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val prefs = getSharedPreferences("p", 0)
        Loc.apply(this)
        val today = LocalDate.now().toEpochDay()
        val il = prefs.getBoolean("il", false)
        val (mOn, tOn) = watchWindows(java.util.Calendar.getInstance(), il, prefs.getInt("m", -1), prefs.getInt("t", -1), prefs.getLong("mEnd", -1), prefs.getLong("tEnd", -1), today)
        val lines = tileLines(System.currentTimeMillis(), il, mOn, tOn, Shema.on(this), Shema.ack(this))
        val grey = 0xFFA9A9B3.toInt()
        val col = LayoutElementBuilders.Column.Builder().setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        fun section(label: String, items: List<String>, max: Int, sp: Float, color: Int) {
            col.addContent(txt(label, 11f, grey))
            if (items.isEmpty()) col.addContent(txt("\u2014", sp, color))
            items.take(max).forEach { col.addContent(txt(it, sp, color)) }
            if (items.size > max) col.addContent(txt("+${items.size - max} more", 11f, grey))
        }
        section(lines[0].first, lines[0].second, 3, 18f, 0xFFE2B84A.toInt())
        col.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f)).build())
        section(lines[1].first, lines[1].second, 2, 14f, 0xFFFFFFFF.toInt())
        val open = ModifiersBuilders.Clickable.Builder().setId("open").setOnClick(
            ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName).setClassName(MainActivity::class.java.name).build()
            ).build()
        ).build()
        val root = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand()).setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(open).build())
            .addContent(col.build()).build()
        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(5 * 60 * 1000L)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
                .build()
        )
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())

    private fun txt(t: String, sp: Float, color: Int) = LayoutElementBuilders.Text.Builder()
        .setText(t).setMaxLines(2)
        .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(sp)).setColor(ColorBuilders.argb(color)).build())
        .build()
}
