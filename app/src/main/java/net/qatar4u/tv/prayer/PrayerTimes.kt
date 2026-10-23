package net.qatar4u.tv.prayer

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import net.qatar4u.tv.data.PrayerMethodId
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A calculation method. Isha is either a fixed number of minutes after Maghrib or a sun angle.
 * Asr uses the standard (Shafi'i, shadow ratio 1) rule in both methods.
 */
data class PrayerMethod(
    val id: PrayerMethodId,
    val fajrAngle: Double,
    val ishaMinutes: Int?,
    val ishaAngle: Double?,
    val ramadanIshaMinutes: Int?,
) {
    companion object {
        /** Umm al-Qura: Fajr 18.5°, Isha 90 minutes after Maghrib, 120 in Ramadan. */
        val UMM_AL_QURA = PrayerMethod(PrayerMethodId.UMM_AL_QURA, 18.5, 90, null, 120)

        /** Qatar (Ministry of Awqaf calendar style): Fajr 18°, Isha 90 minutes after Maghrib all year. */
        val QATAR = PrayerMethod(PrayerMethodId.QATAR, 18.0, 90, null, 90)

        fun of(id: PrayerMethodId) = when (id) {
            PrayerMethodId.UMM_AL_QURA -> UMM_AL_QURA
            PrayerMethodId.QATAR -> QATAR
        }
    }
}

/**
 * Prayer times computed on the device with no network. Sun position from the low-precision formulas of the
 * US Naval Observatory, refined twice at each time. Accurate to about a minute for the Gulf.
 */
object PrayerTimes {

    const val FAJR = 0
    const val SUNRISE = 1
    const val DHUHR = 2
    const val ASR = 3
    const val MAGHRIB = 4
    const val ISHA = 5

    /**
     * The six times (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) for [date], as minutes after local midnight,
     * with [offsets] (minutes, one per time) added. Values can go past 1440 only for Isha in high latitudes.
     */
    fun minutes(
        date: LocalDate,
        lat: Double,
        lon: Double,
        zone: ZoneId,
        method: PrayerMethod = PrayerMethod.UMM_AL_QURA,
        offsets: List<Int> = List(6) { 0 },
        ramadan: Boolean = isRamadan(date, zone),
    ): IntArray {
        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jd = julian(date) - lon / 360.0
        var t = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0)
        repeat(2) {
            val fajr = sunAngleTime(jd, t[0] / 24, method.fajrAngle, lat, true)
            val rise = sunAngleTime(jd, t[1] / 24, 0.833, lat, true)
            val noon = midDay(jd, t[2] / 24)
            val asr = asrTime(jd, t[3] / 24, lat)
            val set = sunAngleTime(jd, t[4] / 24, 0.833, lat, false)
            val isha = method.ishaAngle?.let { sunAngleTime(jd, t[5] / 24, it, lat, false) } ?: set
            t = doubleArrayOf(fajr, rise, noon, asr, set, isha)
        }
        val out = IntArray(6)
        for (i in 0 until 6) out[i] = ((t[i] + tz - lon / 15.0) * 60).roundToInt()
        if (method.ishaAngle == null) {
            out[ISHA] = out[MAGHRIB] + ((if (ramadan) method.ramadanIshaMinutes else method.ishaMinutes) ?: 90)
        }
        for (i in 0 until 6) out[i] += offsets.getOrElse(i) { 0 }
        return out
    }

    /** Index of the next prayer after [nowMin] (sunrise skipped), or [FAJR] when the day's prayers are over. */
    fun next(times: IntArray, nowMin: Int): Int {
        for (i in times.indices) {
            if (i == SUNRISE) continue
            if (times[i] > nowMin) return i
        }
        return FAJR
    }

    /** Minutes from [nowMin] until the next prayer, crossing midnight to tomorrow's [tomorrowFajr]. */
    fun minutesUntilNext(times: IntArray, nowMin: Int, tomorrowFajr: Int): Int {
        val i = next(times, nowMin)
        return if (i == FAJR && nowMin >= times[FAJR]) tomorrowFajr + 1440 - nowMin else times[i] - nowMin
    }

    fun clock(min: Int, use24h: Boolean = false): String {
        val m = ((min % 1440) + 1440) % 1440
        val h = m / 60
        val mm = m % 60
        val hh = if (use24h) h else if (h % 12 == 0) 12 else h % 12
        return (if (use24h && hh < 10) "0$hh" else "$hh") + ":" + (if (mm < 10) "0$mm" else "$mm")
    }

    fun isRamadan(date: LocalDate, zone: ZoneId): Boolean = try {
        hijriParts(date, zone)[1] == IslamicCalendar.RAMADAN
    } catch (e: Throwable) {
        false
    }

    /** {day, month (0-based), year} in the Umm al-Qura calendar. */
    fun hijriParts(date: LocalDate, zone: ZoneId): IntArray {
        val cal = IslamicCalendar(TimeZone.getTimeZone(zone.id))
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        cal.timeInMillis = ZonedDateTime.of(date.atTime(12, 0), zone).toInstant().toEpochMilli()
        return intArrayOf(cal.get(IslamicCalendar.DAY_OF_MONTH), cal.get(IslamicCalendar.MONTH), cal.get(IslamicCalendar.YEAR))
    }

    // ---- astronomy -----------------------------------------------------------------------------------

    internal fun julian(d: LocalDate): Double {
        var y = d.year
        var m = d.monthValue
        if (m <= 2) { y--; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d.dayOfMonth + b - 1524.5
    }

    /** {declination in degrees, equation of time in hours}. */
    private fun sun(jd: Double): DoubleArray {
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(g) + 0.020 * sin(2 * g), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(atan2(cos(e) * sin(l), cos(l)) / 15.0, 24.0)
        val decl = asin(sin(e) * sin(l))
        return doubleArrayOf(decl, q / 15.0 - ra)
    }

    private fun midDay(jd: Double, portion: Double) = fix(12 - sun(jd + portion)[1], 24.0)

    private fun sunAngleTime(jd: Double, portion: Double, angle: Double, lat: Double, ccw: Boolean): Double {
        val decl = sun(jd + portion)[0]
        val noon = midDay(jd, portion)
        val t = acos((-sin(angle) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat))) / 15.0
        return noon + if (ccw) -t else t
    }

    private fun asrTime(jd: Double, portion: Double, lat: Double): Double {
        val decl = sun(jd + portion)[0]
        val angle = -Math.toDegrees(Math.atan(1 / (1 + Math.tan(Math.toRadians(abs(lat - decl))))))
        return sunAngleTime(jd, portion, angle, lat, false)
    }

    private fun fix(a: Double, b: Double): Double { val r = a - b * floor(a / b); return if (r < 0) r + b else r }
    private fun sin(d: Double) = kotlin.math.sin(Math.toRadians(d))
    private fun cos(d: Double) = kotlin.math.cos(Math.toRadians(d))
    private fun asin(x: Double) = Math.toDegrees(kotlin.math.asin(x))
    private fun acos(x: Double) = Math.toDegrees(kotlin.math.acos(x.coerceIn(-1.0, 1.0)))
    private fun atan2(y: Double, x: Double) = Math.toDegrees(kotlin.math.atan2(y, x))
}
