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
    NACHEM("נחם", "Tisha B'Av", "נחם"),
    SHEMA("שמע", "Krias Shema", "שמע")
}

/** The civil date whose Hebrew date applies to this service: evening Maariv (afternoon clock time) uses tomorrow's; after midnight uses today's. */
fun prayerDate(now: Calendar, s: Service): Calendar {
    val cal = now.clone() as Calendar
    if (s == Service.MAARIV && cal.get(Calendar.HOUR_OF_DAY) >= 12) cal.add(Calendar.DAY_OF_MONTH, 1)
    return cal
}

/** Items due for a service. Evening Maariv uses the next Hebrew date (Jewish day starts at nightfall). */
fun autoItems(now: Calendar, s: Service, inIsrael: Boolean, showMashiv: Boolean, showTal: Boolean): List<Item> =
    itemsOn(prayerDate(now, s), s, inIsrael, showMashiv, showTal)

/** Items due for a service on a date already resolved with [prayerDate]. */
private fun itemsOn(date: Calendar, s: Service, inIsrael: Boolean, showMashiv: Boolean, showTal: Boolean): List<Item> {
    val j = JewishCalendar(date).apply { setInIsrael(inIsrael) }
    val out = mutableListOf<Item>()
    if (j.isRoshChodesh || j.isCholHamoed || j.isYomTov) out += Item.YAALEH
    if (j.isAseresYemeiTeshuva) { out += Item.HAKADOSH; out += Item.HAMISHPAT }
    if (showMashiv && j.isMashivHaruachRecited) out += Item.GESHEM
    if (showTal) out += if (j.isVeseinTalUmatarRecited) Item.TAL_UMATAR else Item.BRACHA
    if (j.isChanukah || j.isPurim) out += Item.HANISSIM
    val tishaBav = j.yomTovIndex == JewishCalendar.TISHA_BEAV
    if (j.isTaanis && (s == Service.MINCHA || (s == Service.SHACHARIT && !tishaBav))) out += Item.ANENU // Tisha B'Av: Mincha only, with Nachem
    if (s == Service.MINCHA && tishaBav) out += Item.NACHEM
    return out
}

/** Long-term states (1/0): Mashiv HaRuach vs Morid HaTal, and V'ten Tal U'matar vs V'ten Bracha, for the current service's date
 *  (after shkia that is tomorrow's, so the change shows at the first Maariv, e.g. V'ten Tal U'matar on the evening of 4 Dec). */
fun longTermState(now: Calendar, israel: Boolean): Pair<Int, Int> = stateOn(prayerDate(now, autoService(now.timeInMillis)), israel)

/** Watch season windows (mOn, tOn): on until the stored end date, and also while a season change has not yet been answered in the app. */
fun watchWindows(now: Calendar, israel: Boolean, storedM: Int, storedT: Int, mEnd: Long, tEnd: Long, today: Long): Pair<Boolean, Boolean> {
    val (m, t) = longTermState(now, israel)
    return Pair(mEnd >= today || (storedM != -1 && storedM != m), tEnd >= today || (storedT != -1 && storedT != t))
}

private fun stateOn(date: Calendar, israel: Boolean): Pair<Int, Int> {
    val j = JewishCalendar(date).apply { setInIsrael(israel) }
    return Pair(if (j.isMashivHaruachRecited) 1 else 0, if (j.isVeseinTalUmatarRecited) 1 else 0)
}

/** Where prayer times are worked out: Edgware until an app supplies the device location with [setLocation]. The time zone is always the device's. */
@Volatile private var latLon = Pair(51.6137, -0.2751)

fun setLocation(lat: Double, lon: Double) {
    if (lat in -90.0..90.0 && lon in -180.0..180.0) latLon = Pair(lat, lon)
}

fun currentLatLon(): Pair<Double, Double> = latLon

fun location() = GeoLocation("here", latLon.first, latLon.second, 0.0, TimeZone.getDefault())

/** True when the device time zone is Israel's (used to pick Israel vs diaspora calendar automatically). */
fun isIsraelHere() = TimeZone.getDefault().id in setOf("Asia/Jerusalem", "Asia/Tel_Aviv", "Israel")

/** Shacharit until chatzot, Mincha until shkia, Maariv until alos hashachar. */
fun autoService(nowMs: Long): Service {
    val loc = location()
    val zc = ZmanimCalendar(loc)
    zc.calendar = Calendar.getInstance(loc.timeZone).apply { timeInMillis = nowMs }
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
    return JewishCalendar(prayerDate(now, s)).apply { setInIsrael(inIsrael) }.isPurim
}

val ITEM_ORDER = listOf(Item.SHEMA, Item.HANISSIM, Item.YAALEH, Item.HAKADOSH, Item.HAMISHPAT, Item.NACHEM, Item.ANENU, Item.GESHEM, Item.TAL_UMATAR, Item.BRACHA)

/** Krias Shema windows (optional feature). Morning: sunrise until the latest Shema (Gra). Evening: nightfall (8.5 degrees) until chatzot halayla. */
class ShemaEvent(val at: Long, val id: String, val evening: Boolean, val endsAt: Long)

private class ShemaDay(val dateId: String, val sunrise: Long?, val latest: Long?, val night: Long?, val midnight: Long?)

private fun shemaDay(nowMs: Long, offset: Int): ShemaDay {
    val loc = location()
    val day = Calendar.getInstance(loc.timeZone).apply { timeInMillis = nowMs; add(Calendar.DAY_OF_MONTH, offset) }
    val zc = ZmanimCalendar(loc)
    zc.calendar = day
    val id = "%04d-%02d-%02d".format(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH))
    return ShemaDay(id, zc.sunrise?.time, zc.sofZmanShmaGRA?.time, (zc.tzais ?: zc.tzais72)?.time, zc.chatzos?.time?.plus(12 * 3600_000L))
}

/** Id of the Shema window open now ("<date>M" morning, "<date>E" evening), or null. Evening windows run past midnight until chatzot halayla. */
fun shemaWindow(nowMs: Long): String? {
    for (off in -1..0) {
        val d = shemaDay(nowMs, off)
        if (off == 0 && d.sunrise != null && d.latest != null && nowMs >= d.sunrise && nowMs < d.latest) return d.dateId + "M"
        if (d.night != null && d.midnight != null && nowMs >= d.night && nowMs < d.midnight) return d.dateId + "E"
    }
    return null
}

/** The next time to nudge about Shema after [nowMs]: nightfall (evening), or 30 minutes before the latest Shema (morning). */
fun nextShemaReminder(nowMs: Long): ShemaEvent? =
    (-1..1).flatMap { off ->
        val d = shemaDay(nowMs, off)
        listOfNotNull(
            if (d.latest != null) ShemaEvent(d.latest - 30 * 60_000L, d.dateId + "M", false, d.latest) else null,
            if (d.night != null && d.midnight != null) ShemaEvent(d.night, d.dateId + "E", true, d.midnight) else null
        )
    }.filter { it.at > nowMs }.minByOrNull { it.at }

/** The one insertion the complication should show right now (null = show the Kotel). A Shema window not yet acknowledged ([shemaAck]) wins. */
fun complicationItem(nowMs: Long, israel: Boolean, mOn: Boolean, tOn: Boolean, shemaOn: Boolean = false, shemaAck: String = ""): Item? {
    if (shemaOn) shemaWindow(nowMs)?.let { if (it != shemaAck) return Item.SHEMA }
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val due = autoItems(cal, autoService(nowMs), israel, mOn, tOn)
    return ITEM_ORDER.firstOrNull { it in due }
}

/** Tile data: for now and for the next service, a label plus one Hebrew phrase per insertion (most important first).
 *  A season change between now and the next service (e.g. Mincha on 4 Dec -> Maariv) always shows at the next service. */
fun tileLines(nowMs: Long, israel: Boolean, mOn: Boolean, tOn: Boolean, shemaOn: Boolean = false, shemaAck: String = ""): List<Pair<String, List<String>>> {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    val s = autoService(nowMs)
    val n = Service.values()[(s.ordinal + 1) % 3]
    val date = prayerDate(cal, s)
    // The next Maariv is tonight's, so tomorrow's date even before 12:00 (winter chatzot is ~11:45). Next Shacharit after evening Maariv is already covered by date.
    val nextDate = (cal.clone() as Calendar).apply { if (n == Service.MAARIV) add(Calendar.DAY_OF_MONTH, 1) else timeInMillis = date.timeInMillis }
    val (m, t) = stateOn(date, israel)
    val (m2, t2) = stateOn(nextDate, israel)
    val shemaFirst = if (shemaOn && shemaWindow(nowMs).let { it != null && it != shemaAck }) listOf(Item.SHEMA.heb) else emptyList()
    fun heb(l: List<Item>) = l.sortedBy { ITEM_ORDER.indexOf(it) }.map { it.heb }
    return listOf(
        "NOW \u00b7 ${s.label}" to (shemaFirst + heb(itemsOn(date, s, israel, mOn, tOn))),
        "NEXT \u00b7 ${n.label}" to heb(itemsOn(nextDate, n, israel, mOn || m2 != m, tOn || t2 != t))
    )
}
