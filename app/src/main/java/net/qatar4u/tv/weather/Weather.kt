package net.qatar4u.tv.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.qatar4u.tv.data.SettingsStore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

data class WeatherNow(
    val temperatureC: Double,
    val humidity: Int,
    val windKmh: Double,
    val code: Int,
    val isDay: Boolean,
    /** When it was fetched, epoch milliseconds. */
    val fetchedAt: Long,
)

object Weather {

    private const val ENDPOINT = "https://api.open-meteo.com/v1/forecast"

    /** Older than this, the reading is shown as "last known", not as the current weather. */
    const val STALE_MS = 2 * 60 * 60 * 1000L

    fun url(lat: Double, lon: Double, zone: String) = String.format(
        Locale.US,
        "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,is_day" +
            "&wind_speed_unit=kmh&temperature_unit=celsius&timezone=%s",
        ENDPOINT, lat, lon, zone,
    )

    /** One HTTPS request. Returns the raw JSON so it can be cached as received. */
    suspend fun fetch(lat: Double, lon: Double, zone: String): String = withContext(Dispatchers.IO) {
        val c = URL(url(lat, lon, zone)).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Qatar4uTV/1.0 (Android)")
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun parse(json: String, fetchedAt: Long): WeatherNow? = runCatching {
        val cur = JSONObject(json).getJSONObject("current")
        WeatherNow(
            temperatureC = cur.getDouble("temperature_2m"),
            humidity = cur.optInt("relative_humidity_2m", -1),
            windKmh = cur.optDouble("wind_speed_10m", 0.0),
            code = cur.optInt("weather_code", 0),
            isDay = cur.optInt("is_day", 1) == 1,
            fetchedAt = fetchedAt,
        )
    }.getOrNull()

    /** Fetch and cache. Returns false when the network or the service failed; the old cache stays. */
    suspend fun refresh(context: Context): Boolean {
        val store = SettingsStore(context)
        val s = store.current()
        if (!s.weatherEnabled) return false
        return runCatching {
            val json = fetch(s.lat, s.lon, s.zoneId)
            requireNotNull(parse(json, 0)) { "unexpected response" }
            store.saveWeather(json, System.currentTimeMillis())
        }.isSuccess
    }

    /** Hourly (or the owner's interval) refresh, only with a network. Replaces any earlier schedule. */
    fun schedule(context: Context, minutes: Int, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork(WORK); return }
        val req = PeriodicWorkRequestBuilder<WeatherWorker>(minutes.coerceAtLeast(30).toLong(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    private const val WORK = "weather-refresh"

    /** WMO weather code to a short label. */
    fun describe(code: Int, ar: Boolean): String = when (code) {
        0 -> if (ar) "صافٍ" else "Clear"
        1 -> if (ar) "صافٍ غالبًا" else "Mostly clear"
        2 -> if (ar) "غائم جزئيًا" else "Partly cloudy"
        3 -> if (ar) "غائم" else "Overcast"
        45, 48 -> if (ar) "ضباب" else "Fog"
        51, 53, 55, 56, 57 -> if (ar) "رذاذ" else "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> if (ar) "مطر" else "Rain"
        71, 73, 75, 77, 85, 86 -> if (ar) "ثلج" else "Snow"
        95, 96, 99 -> if (ar) "عاصفة رعدية" else "Thunderstorm"
        else -> if (ar) "—" else "—"
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (Weather.refresh(applicationContext)) Result.success() else Result.retry()
}
