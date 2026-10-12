package com.garry.reminder

import android.app.PendingIntent
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.time.LocalDate

/** Shows the Kotel, or (when an insertion is due) its Hebrew highlight word. */
class SiddurComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType) = build(type, null)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = getSharedPreferences("p", 0)
        val today = LocalDate.now().toEpochDay()
        val item = complicationItem(
            System.currentTimeMillis(), prefs.getBoolean("il", false),
            prefs.getLong("mEnd", -1) >= today, prefs.getLong("tEnd", -1) >= today
        )
        return build(request.complicationType, item)
    }

    private fun build(type: ComplicationType, item: Item?): ComplicationData? {
        val tap = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val desc = PlainComplicationText.Builder(item?.en ?: "Daven reminder").build()
        return when (type) {
            ComplicationType.SMALL_IMAGE -> {
                val icon = if (item != null) textIcon(item.small, false) else Icon.createWithResource(this, R.drawable.ic_kotel)
                SmallImageComplicationData.Builder(SmallImage.Builder(icon, SmallImageType.PHOTO).build(), desc).setTapAction(tap).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                val icon = if (item != null) textIcon(item.small, true) else Icon.createWithResource(this, R.drawable.ic_siddur)
                MonochromaticImageComplicationData.Builder(MonochromaticImage.Builder(icon).build(), desc).setTapAction(tap).build()
            }
            else -> null
        }
    }

    private fun textIcon(text: String, mono: Boolean): Icon {
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (!mono) c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F3A6B.toInt() })
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mono) Color.WHITE else 0xFFE2B84A.toInt()
            textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; textSize = 90f
        }
        while (p.measureText(text) > size * 0.78f && p.textSize > 20f) p.textSize -= 4f
        c.drawText(text, size / 2f, size / 2f - (p.ascent() + p.descent()) / 2f, p)
        return Icon.createWithBitmap(bmp)
    }
}
