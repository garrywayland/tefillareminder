package com.garry.reminder.phone

import android.content.Context
import android.content.SharedPreferences
import com.garry.reminder.*
import java.time.LocalDate
import java.util.Calendar

data class DueState(val service: Service, val items: List<Item>, val purim: Boolean)

/**
 * Season window (Morid HaGeshem, V'ten Tal/Bracha): counts N days from your first open after the change.
 * The widget (record = false) never starts the count; it shows the item until you first open the reminder.
 */
private fun windowOn(prefs: SharedPreferences, key: String, state: Int, today: Long, days: Int, record: Boolean): Boolean {
    if (days <= 0) return false
    val stored = prefs.getInt(key, -1)
    if (stored != state) {
        if (record) prefs.edit().putInt(key, state).putLong(key + "Start", if (stored == -1) -100000L else today).apply()
        else return stored != -1
    }
    return today - prefs.getLong(key + "Start", -100000L) < days
}

fun computeDue(ctx: Context, record: Boolean = false): DueState {
    Loc.apply(ctx)
    val prefs = ctx.getSharedPreferences("p", 0)
    val il = prefs.getBoolean("il", false)
    val days = prefs.getInt("seasonDays", 10) // 0 = never show season items
    val nowMs = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val today = LocalDate.now().toEpochDay()
    val svc = autoService(nowMs)
    val (m, t) = longTermState(cal, il)
    val mOn = windowOn(prefs, "m", m, today, days, record)
    val tOn = windowOn(prefs, "t", t, today, days, record)
    val shemaWin = if (prefs.getInt("shema", 0) > 0) shemaWindow(nowMs) else null // 0 off, 1 widget, 2 widget + notification
    val showShema = shemaWin != null && prefs.getString("shemaAck", "") != shemaWin
    if (record && showShema) prefs.edit().putString("shemaAck", shemaWin).apply() // opening the reminder acknowledges it; the widget then reverts
    val items = (if (showShema) listOf(Item.SHEMA) else emptyList()) + autoItems(cal, svc, il, mOn, tOn).sortedBy { ITEM_ORDER.indexOf(it) }
    return DueState(svc, items, isPurim(cal, svc, il))
}
