package com.garry.reminder.phone

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Silences notifications while the reminder is open, and always puts the previous setting back. */
object DndGuard {
    private fun pi(c: Context) = PendingIntent.getBroadcast(c, 7, Intent(c, DndRestoreReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)

    fun enable(c: Context): Boolean {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return false
        val prefs = c.getSharedPreferences("p", 0)
        if (!prefs.getBoolean("dndOn", false)) {
            prefs.edit().putInt("prevFilter", nm.currentInterruptionFilter).putBoolean("dndOn", true).apply()
        }
        val level = when (prefs.getInt("dndLevel", 0)) {
            1 -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
            2 -> NotificationManager.INTERRUPTION_FILTER_NONE
            else -> NotificationManager.INTERRUPTION_FILTER_ALARMS
        }
        nm.setInterruptionFilter(level)
        prefs.edit().putInt("ourFilter", level).apply()
        val minutes = prefs.getInt("dndMinutes", 45)
        c.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pi(c))
        return true
    }

    fun restore(c: Context) {
        val prefs = c.getSharedPreferences("p", 0)
        if (!prefs.getBoolean("dndOn", false)) return
        val nm = c.getSystemService(NotificationManager::class.java)
        // Only put things back if you haven't changed Do Not Disturb yourself since we set it.
        if (nm.isNotificationPolicyAccessGranted && nm.currentInterruptionFilter == prefs.getInt("ourFilter", -1)) {
            val prev = prefs.getInt("prevFilter", NotificationManager.INTERRUPTION_FILTER_ALL)
            nm.setInterruptionFilter(if (prev in 1..4) prev else NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        prefs.edit().putBoolean("dndOn", false).apply()
        c.getSystemService(AlarmManager::class.java).cancel(pi(c))
    }
}

class DndRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) = DndGuard.restore(c)
}
