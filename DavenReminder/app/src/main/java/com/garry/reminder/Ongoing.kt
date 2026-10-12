package com.garry.reminder

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity

object Ongoing {
    private const val ICON = android.R.drawable.ic_lock_idle_alarm
    fun start(c: Context, text: String) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("d", "Davening", NotificationManager.IMPORTANCE_LOW))
        val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(c, "d").setSmallIcon(ICON).setContentTitle("Davening")
            .setContentText(text).setOngoing(true).setCategory(NotificationCompat.CATEGORY_STATUS).setContentIntent(pi)
        OngoingActivity.Builder(c, 1, b).setStaticIcon(ICON).setTouchIntent(pi).build().apply(c)
        nm.notify(1, b.build())
    }
    fun stop(c: Context) = c.getSystemService(NotificationManager::class.java).cancel(1)
}
