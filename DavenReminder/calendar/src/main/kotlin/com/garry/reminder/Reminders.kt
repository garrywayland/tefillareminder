package com.garry.reminder

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.ZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.util.Calendar
import java.util.TimeZone

enum class Service(val label: String) { SHACHARIT("Shacharit"), MINCHA("Mincha"), MAARIV("Maariv") }

enum class Item(val heb: String, val en: String, val small: String) {
    YAALEH("יעלה ויבוא", "Rosh Chodesh / Yom Tov / Chol Hamoed", "יעלה"),
    HAKADOSH("המלך הקדוש", "Aseret Yemei Teshuva", "המלך"),
    HAMISHPAT("המלך המשפט", "Aseret Yemei Teshuva", "המלך"),
    GESHEM("מוריד הגשם", "Winter", "הגשם"),
    TAL_UMATAR("ותן טל ומטר", "Barech Aleinu", "טל ומטר"),
    BRACHA("ותן ברכה", "Barech Aleinu", "ברכה"),
    HANISSIM("על הנסים", "Chanukah / Purim", "הנסים"),
    ANENU("עננו", "Fast day", "עננו"),
    NACHEM("נחם", "Tisha B'Av", "נחם")
}

/** Items due for a service. Evening Maariv uses the next Hebrew date (Jewish day starts at nightfall). */
fun autoItems(now: Calendar, s: Service, inIsrael: Boolean, showMashiv: Boolean, showTal: Boolean): List<Item> {
    val cal = now.clone() as Calendar
    if (s == Service.MAARIV && cal.get(Calendar.HOUR_OF_DAY) >= 12) cal.add(Calendar.DAY_OF_MONTH, 1) // evening Maariv (afternoon clock time) uses tomorrow's date; after midnight uses today's
    val j = JewishCalendar(cal).apply { setInIsrael(inIsrael) }
    val out = mutableListOf<Item>()
    if (j.isRoshChodesh || j.isCholHamoed || j.isYomTov) out += Item.YAALEH
    if (j.isAseresYemeiTeshuva) { out += Item.HAKADOSH; out += Item.HAMISHPAT }
    if (showMashiv && j.isMashivHaruachRecited) out += Item.GESHEM
    if (showTal) out += if (j.isVeseinTalUmatarRecited) Item.TAL_UMATAR else Item.BRACHA
    if (j.isChanukah || j.isPurim) out += Item.HANISSIM
    if (s != Service.MAARIV && j.isTaanis && j.yomTovIndex != JewishCalendar.TISHA_BEAV) out += Item.ANENU
    if (s == Service.MINCHA && j.yomTovIndex == JewishCalendar.TISHA_BEAV) out += Item.NACHEM
    return out
}

/** Long-term states (1/0): Mashiv HaRuach vs Morid HaTal, and V'ten Tal U'matar vs V'ten Bracha, for today's date. */
fun longTermState(now: Calendar, israel: Boolean): Pair<Int, Int> {
    val j = JewishCalendar(now).apply { setInIsrael(israel) }
    return Pair(if (j.isMashivHaruachRecited) 1 else 0, if (j.isVeseinTalUmatarRecited) 1 else 0)
}

/** Location used for prayer times (Edgware, London). Change these to move the watch elsewhere. */
val LOCATION = GeoLocation("Edgware", 51.6137, -0.2751, 0.0, TimeZone.getTimeZone("Europe/London"))

/** Shacharit until chatzot, Mincha until shkia, Maariv until alos hashachar. */
fun autoService(nowMs: Long): Service {
    val zc = ZmanimCalendar(LOCATION)
    zc.calendar = Calendar.getInstance(LOCATION.timeZone).apply { timeInMillis = nowMs }
    val alos = zc.alosHashachar?.time ?: zc.alos72?.time // summer: no 16.1° dawn, fall back to sunrise minus 72 min
    val chatzos = zc.chatzos?.time
    val shkia = zc.sunset?.time
    return when {
        alos != null && nowMs < alos -> Service.MAARIV
        chatzos != null && nowMs < chatzos -> Service.SHACHARIT
        shkia != null && nowMs < shkia -> Service.MINCHA
        else -> Service.MAARIV
    }
}

/** True when today's (or, for evening Maariv, tomorrow's) Hebrew date is Purim. */
fun isPurim(now: Calendar, s: Service, inIsrael: Boolean): Boolean {
    val cal = now.clone() as Calendar
    if (s == Service.MAARIV && cal.get(Calendar.HOUR_OF_DAY) >= 12) cal.add(Calendar.DAY_OF_MONTH, 1)
    return JewishCalendar(cal).apply { setInIsrael(inIsrael) }.isPurim
}

val ITEM_ORDER = listOf(Item.HANISSIM, Item.YAALEH, Item.HAKADOSH, Item.HAMISHPAT, Item.NACHEM, Item.ANENU, Item.GESHEM, Item.TAL_UMATAR, Item.BRACHA)

/** The one insertion the complication should show right now (null = show the Kotel). */
fun complicationItem(nowMs: Long, israel: Boolean, mOn: Boolean, tOn: Boolean): Item? {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val due = autoItems(cal, autoService(nowMs), israel, mOn, tOn)
    return ITEM_ORDER.firstOrNull { it in due }
}

/** Tile data: for now and for the next service, a label plus one Hebrew phrase per insertion (most important first). */
fun tileLines(nowMs: Long, israel: Boolean, mOn: Boolean, tOn: Boolean): List<Pair<String, List<String>>> {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val s = autoService(nowMs)
    val n = Service.values()[(s.ordinal + 1) % 3]
    val cal2 = cal.clone() as Calendar
    if (s == Service.MAARIV && cal.get(Calendar.HOUR_OF_DAY) >= 12) cal2.add(Calendar.DAY_OF_MONTH, 1)
    fun heb(l: List<Item>) = l.sortedBy { ITEM_ORDER.indexOf(it) }.map { it.heb }
    return listOf(
        "NOW \u00b7 ${s.label}" to heb(autoItems(cal, s, israel, mOn, tOn)),
        "NEXT \u00b7 ${n.label}" to heb(autoItems(cal2, n, israel, mOn, tOn))
    )
}
