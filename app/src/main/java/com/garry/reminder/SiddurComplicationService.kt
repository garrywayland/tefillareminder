package com.garry.reminder

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class SiddurComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType) = build(type)
    override suspend fun onComplicationRequest(request: ComplicationRequest) = build(request.complicationType)

    private fun build(type: ComplicationType): ComplicationData? {
        val tap = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val desc = PlainComplicationText.Builder("Daven reminder").build()
        val mono = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_siddur)).build()
        return when (type) {
            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                SmallImage.Builder(Icon.createWithResource(this, R.drawable.ic_siddur_color), SmallImageType.ICON).build(), desc
            ).setTapAction(tap).build()
            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(mono, desc).setTapAction(tap).build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(PlainComplicationText.Builder("Daven").build(), desc)
                .setMonochromaticImage(mono).setTapAction(tap).build()
            else -> null
        }
    }
}
