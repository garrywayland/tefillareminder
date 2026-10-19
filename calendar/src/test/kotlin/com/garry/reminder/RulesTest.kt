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
        expect("Tisha B'Av Mincha", at(2027, 8, 12, 14), M, listOf(Item.NACHEM, Item.ANENU))
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
        // Season change must show at the first Maariv (evening of 4 Dec), not the next morning
        if (longTermState(Calendar.getInstance().apply { timeInMillis = ms(2026, 12, 4, 19, 30) }, false) != Pair(1, 1)) fails += "4 Dec Maariv: long-term state should already be V'ten Tal U'matar"
        if (longTermState(Calendar.getInstance().apply { timeInMillis = ms(2026, 12, 4, 14) }, false) != Pair(1, 0)) fails += "4 Dec Mincha: long-term state should still be V'ten Bracha"
        if (longTermState(Calendar.getInstance().apply { timeInMillis = ms(2026, 10, 17, 19, 30) }, true) != Pair(1, 1)) fails += "Israel eve of 7 Cheshvan: should be V'ten Tal U'matar"
        // Watch: an unanswered change counts as on; an answered, expired window does not
        if (watchWindows(Calendar.getInstance().apply { timeInMillis = ms(2026, 12, 4, 19, 30) }, false, 1, 0, 0, 0, 20000) != Pair(false, true)) fails += "Watch windows 4 Dec Maariv: V'ten Tal should be on before answering"
        if (watchWindows(Calendar.getInstance().apply { timeInMillis = ms(2026, 12, 4, 14) }, false, 1, 0, 0, 0, 20000) != Pair(false, false)) fails += "Watch windows 4 Dec Mincha: nothing should be on"
        // Tile NEXT: Mincha at 11:55 on 4 Dec (after winter chatzot, before 12:00) -> tonight's Maariv is 25 Kislev, first V'ten Tal U'matar
        val tile = tileLines(ms(2026, 12, 4, 11, 55), false, false, false)
        if (!tile[0].first.contains("Mincha")) fails += "Tile 4 Dec 11:55: NOW should be Mincha (got ${tile[0].first})"
        listOf(Item.HANISSIM, Item.TAL_UMATAR).filter { it.heb !in tile[1].second }.forEach { fails += "Tile 4 Dec 11:55: NEXT Maariv missing $it (got ${tile[1].second})" }
        if (Item.BRACHA.heb in tile[1].second) fails += "Tile 4 Dec 11:55: NEXT Maariv should not show V'ten Bracha"
        val tile2 = tileLines(ms(2026, 12, 4, 19, 30), false, false, true)
        if (Item.TAL_UMATAR.heb !in tile2[0].second || Item.HANISSIM.heb !in tile2[0].second) fails += "Tile 4 Dec 19:30: NOW Maariv should have Tal U'matar and Al HaNissim (got ${tile2[0].second})"
        if (Item.HANISSIM.heb !in tile2[1].second) fails += "Tile 4 Dec 19:30: NEXT Shacharit should have Al HaNissim (got ${tile2[1].second})"
        // Location: Jerusalem on 4 Dec, 16:20 local is before shkia (~16:36) -> Mincha; Edgware would already be Maariv
        val jer = TimeZone.getTimeZone("Asia/Jerusalem")
        try {
            TimeZone.setDefault(jer); setLocation(31.7683, 35.2137)
            val tj = Calendar.getInstance(jer).apply { clear(); set(2026, 11, 4, 16, 20) }.timeInMillis
            if (autoService(tj) != Service.MINCHA) fails += "Jerusalem 4 Dec 16:20 should be Mincha (got ${autoService(tj)})"
            if (!isIsraelHere()) fails += "Asia/Jerusalem time zone should count as Israel"
        } finally { TimeZone.setDefault(tz); setLocation(51.6137, -0.2751) }
        if (isIsraelHere()) fails += "Europe/London must not count as Israel"
        // Shema windows (Edgware): 4 Dec sunrise 07:49, latest Shema (Gra) 09:50, nightfall (8.5 deg) 16:51, chatzot 11:51 so chatzot halayla 23:51
        fun win(h: Int, mi: Int, d: Int = 4) = shemaWindow(ms(2026, 12, d, h, mi))
        if (win(6, 0) != null) fails += "Shema 4 Dec 06:00 should be outside any window (got ${win(6, 0)})"
        if (win(8, 30) != "2026-12-04M") fails += "Shema 4 Dec 08:30 should be morning (got ${win(8, 30)})"
        if (win(10, 0) != null) fails += "Shema 4 Dec 10:00 is after the latest morning Shema (got ${win(10, 0)})"
        if (win(15, 0) != null) fails += "Shema 4 Dec 15:00 should be outside any window (got ${win(15, 0)})"
        if (win(20, 0) != "2026-12-04E") fails += "Shema 4 Dec 20:00 should be evening (got ${win(20, 0)})"
        if (win(23, 0) != "2026-12-04E") fails += "Shema 4 Dec 23:00 should still be evening"
        if (win(0, 30, 5) != null) fails += "Shema 5 Dec 00:30 is after chatzot halayla (23:51) (got ${win(0, 30, 5)})"
        // Summer (BST): chatzot 13:02 so chatzot halayla is 01:02 the next morning
        val sj = shemaWindow(ms(2027, 6, 22, 0, 30))
        if (sj != "2027-06-21E") fails += "Shema 22 Jun 00:30 BST should still be the 21 Jun evening window (got $sj)"
        if (shemaWindow(ms(2027, 6, 22, 2, 0)) != null) fails += "Shema 22 Jun 02:00 BST is after chatzot halayla"
        val nx = nextShemaReminder(ms(2026, 12, 4, 6, 0))
        if (nx == null || nx.evening || nx.id != "2026-12-04M" || nx.at < ms(2026, 12, 4, 9, 10) || nx.at > ms(2026, 12, 4, 9, 30)) fails += "Next Shema reminder from 06:00 should be 30 min before the latest morning Shema (got ${nx?.id} at ${nx?.at})"
        val nx2 = nextShemaReminder(ms(2026, 12, 4, 12, 0))
        if (nx2 == null || !nx2.evening || nx2.id != "2026-12-04E") fails += "Next Shema reminder from noon should be tonight's nightfall (got ${nx2?.id})"
        if (complicationItem(ms(2026, 12, 4, 8, 30), false, false, false, shemaOn = true) != Item.SHEMA) fails += "Complication should show Shema in the morning window"
        if (complicationItem(ms(2026, 12, 4, 8, 30), false, false, false, shemaOn = true, shemaAck = "2026-12-04M") == Item.SHEMA) fails += "Complication should revert once Shema is acknowledged"
        if (complicationItem(ms(2026, 12, 4, 8, 30), false, false, false) == Item.SHEMA) fails += "Shema must stay off when the option is off"
        if (Item.SHEMA.heb !in tileLines(ms(2026, 12, 4, 20, 0), false, false, false, shemaOn = true)[0].second) fails += "Tile should list Shema in the evening window"
        if (Item.SHEMA.heb in tileLines(ms(2026, 12, 4, 20, 0), false, false, false)[0].second) fails += "Tile must not list Shema when the option is off"
        assertTrue(fails.isEmpty(), "\n" + fails.joinToString("\n"))
    }
}
