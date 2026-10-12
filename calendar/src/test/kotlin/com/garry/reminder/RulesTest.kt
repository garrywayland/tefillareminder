package com.garry.reminder

import java.util.Calendar
import java.util.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class RulesTest {
    private val tz = TimeZone.getTimeZone("Europe/London")
    private val fails = mutableListOf<String>()

    @BeforeTest fun setTz() { TimeZone.setDefault(tz) }

    private fun ms(y: Int, m: Int, d: Int, h: Int, mi: Int = 0) =
        Calendar.getInstance(tz).apply { clear(); set(y, m - 1, d, h, mi) }.timeInMillis

    private fun at(y: Int, m: Int, d: Int, h: Int, mi: Int = 0, il: Boolean = false, win: Boolean = true): Pair<Service, List<Item>> {
        val t = ms(y, m, d, h, mi)
        val s = autoService(t)
        return s to autoItems(Calendar.getInstance().apply { timeInMillis = t }, s, il, win, win)
    }

    private fun expect(label: String, r: Pair<Service, List<Item>>, svc: Service? = null, has: List<Item> = emptyList(), not: List<Item> = emptyList()) {
        if (svc != null && r.first != svc) fails += "$label: service was ${r.first}, expected $svc"
        has.filter { it !in r.second }.forEach { fails += "$label: missing $it (got ${r.second})" }
        not.filter { it in r.second }.forEach { fails += "$label: unexpected $it (got ${r.second})" }
    }

    @Test fun transitions() {
        val S = Service.SHACHARIT; val M = Service.MINCHA; val V = Service.MAARIV
        expect("1 Tishrei", at(2026, 9, 12, 9), S, listOf(Item.YAALEH, Item.HAKADOSH))
        expect("Tzom Gedalya", at(2026, 9, 14, 9), S, listOf(Item.ANENU, Item.HAKADOSH), listOf(Item.YAALEH))
        expect("Erev Rosh Chodesh Maariv", at(2026, 10, 10, 20), V, listOf(Item.YAALEH))
        expect("30 Tishrei", at(2026, 10, 11, 9), S, listOf(Item.YAALEH))
        expect("4 Dec morning", at(2026, 12, 4, 10), S, listOf(Item.GESHEM, Item.BRACHA), listOf(Item.TAL_UMATAR))
        expect("4 Dec Mincha", at(2026, 12, 4, 14), M, listOf(Item.BRACHA), listOf(Item.TAL_UMATAR))
        expect("4 Dec Maariv", at(2026, 12, 4, 19, 30), V, listOf(Item.TAL_UMATAR, Item.HANISSIM), listOf(Item.BRACHA))
        expect("6 Dec", at(2026, 12, 6, 9), S, listOf(Item.TAL_UMATAR, Item.HANISSIM))
        expect("Fast of Esther Shacharit", at(2027, 3, 22, 9), S, listOf(Item.ANENU))
        expect("Fast of Esther Mincha", at(2027, 3, 22, 15), M, listOf(Item.ANENU))
        expect("Fast of Esther Maariv = Purim eve", at(2027, 3, 22, 20), V, listOf(Item.HANISSIM), listOf(Item.ANENU))
        expect("Purim", at(2027, 3, 23, 9), S, listOf(Item.HANISSIM))
        expect("Erev Pesach Mincha", at(2027, 4, 21, 15), M, listOf(Item.TAL_UMATAR), listOf(Item.BRACHA))
        expect("Chol Hamoed Pesach", at(2027, 4, 26, 9), S, listOf(Item.YAALEH, Item.BRACHA), listOf(Item.GESHEM, Item.TAL_UMATAR))
        expect("Tisha B'Av Mincha", at(2027, 8, 12, 14), M, listOf(Item.NACHEM), listOf(Item.ANENU))
        expect("Tisha B'Av Shacharit", at(2027, 8, 12, 9), S, not = listOf(Item.NACHEM, Item.ANENU))
        expect("Israel 6 Cheshvan", at(2026, 10, 17, 9, il = true), S, listOf(Item.BRACHA), listOf(Item.TAL_UMATAR))
        expect("Israel 7 Cheshvan", at(2026, 10, 18, 9, il = true), S, listOf(Item.TAL_UMATAR))
        expect("Windows off", at(2026, 12, 6, 9, win = false), S, listOf(Item.HANISSIM), listOf(Item.TAL_UMATAR, Item.BRACHA, Item.GESHEM))
        expect("Dec 03:00", at(2026, 12, 4, 3), V)
        expect("Dec 08:00", at(2026, 12, 4, 8), S)
        expect("Dec 14:00", at(2026, 12, 4, 14), M)
        expect("Dec 17:00", at(2026, 12, 4, 17), V)
        expect("June 02:00 (no 16.1 dawn)", at(2027, 6, 21, 2), V)
        expect("June 05:00", at(2027, 6, 21, 5), S)
        if (complicationItem(ms(2027, 3, 23, 9), false, true, true) != Item.HANISSIM) fails += "Complication on Purim should be Al HaNissim"
        if (complicationItem(ms(2026, 12, 4, 10), false, true, true) != Item.GESHEM) fails += "Complication 4 Dec morning should be Morid HaGeshem"
        assertTrue(fails.isEmpty(), "\n" + fails.joinToString("\n"))
    }
}
