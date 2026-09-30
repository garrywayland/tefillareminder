package com.garry.reminder

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.util.Calendar

enum class Service(val label: String) { SHACHARIT("Shacharit"), MINCHA("Mincha"), MAARIV("Maariv") }

enum class Item(val heb: String, val en: String) {
    YAALEH("יעלה ויבוא", "Rosh Chodesh / Yom Tov / Chol Hamoed"),
    HAKADOSH("הַמֶּלֶךְ הַקָּדוֹשׁ", "Aseret Yemei Teshuva"),
    HAMISHPAT("הַמֶּלֶךְ הַמִּשְׁפָּט", "Aseret Yemei Teshuva"),
    MASHIV("מַשִּׁיב הָרוּחַ", "Winter"),
    MORID("מוֹרִיד הַטָּל", "Summer"),
    TAL_UMATAR("וְתֵן טַל וּמָטָר", "Barech Aleinu"),
    BRACHA("וְתֵן בְּרָכָה", "Barech Aleinu"),
    HANISSIM("על הנסים", "Chanukah / Purim"),
    ANENU("עננו", "Fast day"),
    NACHEM("נחם", "Tisha B'Av")
}

/** Items due for a service. Maariv uses the next Hebrew date (Jewish day starts at nightfall). */
fun autoItems(now: Calendar, s: Service, inIsrael: Boolean, showMashiv: Boolean, showTal: Boolean): List<Item> {
    val cal = now.clone() as Calendar
    if (s == Service.MAARIV) cal.add(Calendar.DAY_OF_MONTH, 1)
    val j = JewishCalendar(cal).apply { setInIsrael(inIsrael) }
    val out = mutableListOf<Item>()
    if (j.isRoshChodesh || j.isCholHamoed || j.isYomTov) out += Item.YAALEH
    if (j.isAseresYemeiTeshuva) { out += Item.HAKADOSH; out += Item.HAMISHPAT }
    if (showMashiv) out += if (j.isMashivHaruachRecited) Item.MASHIV else Item.MORID
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
