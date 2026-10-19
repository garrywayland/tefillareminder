package com.garry.reminder.phone

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.garry.reminder.nextShemaReminder
import com.garry.reminder.shemaWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Optional Shema reminders. Pref "shema": 0 off, 1 widget only, 2 widget + notification (nightfall; 30 min before the latest morning Shema). */
object ShemaAlarms {
    const val NOTIFICATION_ID = 2

    private fun pending(ctx: Context, extras: Intent.() -> Unit = {}) = PendingIntent.getBroadcast(
        ctx, 7, Intent(ctx, ShemaReceiver::class.java).apply(extras), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Sets (or cancels) the next alarm. Inexact (no special permission), so it can fire a few minutes late when the phone is dozing. */
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx))
        if (ctx.getSharedPreferences("p", 0).getInt("shema", 0) < 1) return
        Loc.apply(ctx)
        val ev = nextShemaReminder(System.currentTimeMillis()) ?: return
        val pi = pending(ctx) { putExtra("id", ev.id); putExtra("evening", ev.evening); putExtra("endsAt", ev.endsAt) }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ev.at, pi)
    }

    fun refreshWidget(ctx: Context, then: () -> Unit = {}) {
        CoroutineScope(Dispatchers.Default).launch {
            try { DavenWidget().updateAll(ctx) } finally { then() }
        }
    }
}

class ShemaReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(ctx: Context, intent: Intent) {
        val prefs = ctx.getSharedPreferences("p", 0)
        Loc.apply(ctx)
        val now = System.currentTimeMillis()
        val id = shemaWindow(now) ?: shemaWindow(now + 120_000) // tolerate firing a moment early
        if (prefs.getInt("shema", 0) >= 2 && id != null && prefs.getString("shemaAck", "") != id) {
            val evening = id.endsWith("E")
            val ends = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(intent.getLongExtra("endsAt", now)))
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("shema", "Shema", NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(ctx, 8, Intent(ctx, ReminderActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val n = NotificationCompat.Builder(ctx, "shema")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("\u05e9\u05de\u05e2 \u00b7 " + if (evening) "Evening Shema" else "Morning Shema")
                .setContentText(if (evening) "Nightfall has passed. Say Shema until chatzot halayla ($ends)." else "Latest time for Shema is $ends.")
                .setContentIntent(open).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
            if (NotificationManagerCompat.from(ctx).areNotificationsEnabled()) NotificationManagerCompat.from(ctx).notify(ShemaAlarms.NOTIFICATION_ID, n)
        }
        ShemaAlarms.schedule(ctx)
        val pr = goAsync()
        ShemaAlarms.refreshWidget(ctx) { pr.finish() }
    }
}

/** Re-arms the alarm after a restart, an app update, or a time-zone change. */
class ShemaBootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        ShemaAlarms.schedule(ctx)
        val pr = goAsync()
        ShemaAlarms.refreshWidget(ctx) { pr.finish() }
    }
}
