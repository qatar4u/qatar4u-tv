package net.qatar4u.tv

import net.qatar4u.tv.prayer.PrayerMethod
import net.qatar4u.tv.prayer.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Checked against published Umm al-Qura times for Doha (within 3 minutes). */
class PrayerTimesTest {

    private val qatar = ZoneId.of("Asia/Qatar")
    private fun min(h: Int, m: Int) = h * 60 + m

    private fun near(what: String, expected: Int, actual: Int) =
        assertTrue("$what: expected about ${PrayerTimes.clock(expected)}, got ${PrayerTimes.clock(actual)}",
            kotlin.math.abs(expected - actual) <= 3)

    @Test
    fun dohaInOctober() {
        val t = PrayerTimes.minutes(LocalDate.of(2026, 10, 23), 25.2854, 51.5310, qatar, ramadan = false)
        near("Fajr", min(4, 17), t[0])
        near("Sunrise", min(5, 35), t[1])
        near("Dhuhr", min(11, 18), t[2])
        near("Asr", min(14, 35), t[3])
        near("Maghrib", min(17, 0), t[4])
        assertEquals(90, t[5] - t[4])
    }

    @Test
    fun ishaIsTwoHoursAfterMaghribInRamadan() {
        val t = PrayerTimes.minutes(LocalDate.of(2027, 2, 20), 25.2854, 51.5310, qatar, ramadan = true)
        assertEquals(120, t[5] - t[4])
    }

    @Test
    fun qatarMethodHasALaterFajr() {
        val day = LocalDate.of(2026, 10, 23)
        val uaq = PrayerTimes.minutes(day, 25.2854, 51.5310, qatar, PrayerMethod.UMM_AL_QURA, ramadan = false)
        val qa = PrayerTimes.minutes(day, 25.2854, 51.5310, qatar, PrayerMethod.QATAR, ramadan = false)
        val d = qa[0] - uaq[0]
        assertTrue("half a degree is 2 to 3 minutes here, got $d", d in 1..4)
        for (i in 1..4) assertEquals(uaq[i], qa[i])
    }

    @Test
    fun offsetsAreAddedPerPrayer() {
        val day = LocalDate.of(2026, 10, 23)
        val base = PrayerTimes.minutes(day, 25.2854, 51.5310, qatar, ramadan = false)
        val moved = PrayerTimes.minutes(day, 25.2854, 51.5310, qatar, offsets = listOf(2, 0, 0, -1, 3, 0), ramadan = false)
        assertEquals(base[0] + 2, moved[0])
        assertEquals(base[3] - 1, moved[3])
        assertEquals(base[4] + 3, moved[4])
    }

    @Test
    fun timesAreInOrderAllYear() {
        for (month in 1..12) {
            val t = PrayerTimes.minutes(LocalDate.of(2027, month, 15), 25.2854, 51.5310, qatar, ramadan = false)
            for (i in 1 until t.size) assertTrue("month $month", t[i] > t[i - 1])
        }
    }

    @Test
    fun nextSkipsSunriseAndWraps() {
        val t = intArrayOf(min(4, 20), min(5, 39), min(11, 21), min(14, 37), min(17, 3), min(18, 33))
        assertEquals(0, PrayerTimes.next(t, min(1, 0)))
        assertEquals(2, PrayerTimes.next(t, min(4, 30)))
        assertEquals(5, PrayerTimes.next(t, min(18, 0)))
        assertEquals(0, PrayerTimes.next(t, min(23, 0)))
        assertEquals(min(4, 21) + 1440 - min(23, 0), PrayerTimes.minutesUntilNext(t, min(23, 0), min(4, 21)))
        assertEquals(min(11, 21) - min(6, 0), PrayerTimes.minutesUntilNext(t, min(6, 0), min(4, 21)))
    }

    @Test
    fun clockFormats() {
        assertEquals("4:05", PrayerTimes.clock(min(4, 5)))
        assertEquals("12:00", PrayerTimes.clock(min(12, 0)))
        assertEquals("6:33", PrayerTimes.clock(min(18, 33)))
        assertEquals("18:33", PrayerTimes.clock(min(18, 33), use24h = true))
        assertEquals("04:05", PrayerTimes.clock(min(4, 5), use24h = true))
    }
}
