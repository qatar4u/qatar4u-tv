package net.qatar4u.tv.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.qatar4u.tv.data.AppSettings
import net.qatar4u.tv.prayer.PrayerMethod
import net.qatar4u.tv.prayer.PrayerTimes
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * Dimmed wallpaper with a clock and the next prayer. The text block drifts to a new place every minute,
 * so nothing stays lit in one spot on the AMOLED panel.
 */
@Composable
fun Screensaver(s: AppSettings, now: Long, wallpaper: Bitmap?) {
    val zone = runCatching { ZoneId.of(s.zoneId) }.getOrDefault(ZoneId.of("Asia/Qatar"))
    val z = Instant.ofEpochMilli(now).atZone(zone)
    val nowMin = z.hour * 60 + z.minute
    val method = PrayerMethod.of(s.prayerMethod)
    val times = PrayerTimes.minutes(z.toLocalDate(), s.lat, s.lon, zone, method, s.offsets)
    val tomorrowFajr = PrayerTimes.minutes(z.toLocalDate().plusDays(1), s.lat, s.lon, zone, method, s.offsets)[0]
    val next = PrayerTimes.next(times, nowMin)
    val names = prayerNames()
    Box(Modifier.fillMaxSize()) {
        WallpaperLayer(wallpaper, dim = 0.72f)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val step = (now / 60_000L).toInt()
            val fx = (abs((step * 37) % 100)) / 100f
            val fy = (abs((step * 53) % 100)) / 100f
            Column(Modifier.offset(x = (maxWidth - 420.dp) * fx, y = (maxHeight - 220.dp) * fy)) {
                Text(PrayerTimes.clock(nowMin, s.use24h), style = MaterialTheme.typography.displayLarge, color = Q4u.TextPrimary.copy(alpha = 0.85f))
                Text(
                    stringResourceNext(names[next], durationText(PrayerTimes.minutesUntilNext(times, nowMin, tomorrowFajr))),
                    style = MaterialTheme.typography.titleLarge, color = LocalAccent.current.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun stringResourceNext(name: String, left: String) =
    androidx.compose.ui.res.stringResource(net.qatar4u.tv.R.string.next_prayer, name, left)
