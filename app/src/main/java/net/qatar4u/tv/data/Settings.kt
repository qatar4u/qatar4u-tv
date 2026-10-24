package net.qatar4u.tv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class Accent { GOLD, MAROON, PEARL }

/** Everything the owner can change. Kept small and flat so it backs up to one JSON object. */
data class AppSettings(
    val language: String = "ar",
    val cityId: String = "doha",
    val customName: String = "",
    val lat: Double = Cities.DOHA.lat,
    val lon: Double = Cities.DOHA.lon,
    val zoneId: String = "Asia/Qatar",
    val prayerMethod: PrayerMethodId = PrayerMethodId.UMM_AL_QURA,
    /** Minutes added to Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    val offsets: List<Int> = List(6) { 0 },
    val weatherEnabled: Boolean = true,
    val weatherMinutes: Int = 60,
    val wallpaperMinutes: Int = 30,
    /** File name of a pinned wallpaper, empty to rotate. */
    val pinnedWallpaper: String = "",
    val accent: Accent = Accent.GOLD,
    val screensaverMinutes: Int = 10,
    val pinHash: String = "",
    /** Package order of the app hub; packages not listed follow in name order. */
    val appOrder: List<String> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    /** Apps the owner put in the entertainment row, and ones taken out of it. */
    val entertainment: Set<String> = emptySet(),
    val notEntertainment: Set<String> = emptySet(),
    val tvSafeArea: Int = 0,
    val use24h: Boolean = false,
)

enum class PrayerMethodId { UMM_AL_QURA, QATAR }

class SettingsStore(private val context: Context) {

    private object K {
        val language = stringPreferencesKey("language")
        val cityId = stringPreferencesKey("city_id")
        val customName = stringPreferencesKey("custom_name")
        val lat = doublePreferencesKey("lat")
        val lon = doublePreferencesKey("lon")
        val zone = stringPreferencesKey("zone")
        val method = stringPreferencesKey("prayer_method")
        val offsets = stringPreferencesKey("offsets")
        val weatherEnabled = booleanPreferencesKey("weather_enabled")
        val weatherMinutes = intPreferencesKey("weather_minutes")
        val wallpaperMinutes = intPreferencesKey("wallpaper_minutes")
        val pinned = stringPreferencesKey("pinned_wallpaper")
        val accent = stringPreferencesKey("accent")
        val screensaver = intPreferencesKey("screensaver_minutes")
        val pin = stringPreferencesKey("pin_hash")
        val order = stringPreferencesKey("app_order")
        val favorites = stringPreferencesKey("favorites")
        val hidden = stringPreferencesKey("hidden")
        val entertainment = stringPreferencesKey("entertainment")
        val notEntertainment = stringPreferencesKey("not_entertainment")
        val safeArea = intPreferencesKey("tv_safe_area")
        val use24h = booleanPreferencesKey("use_24h")

        // Weather cache (not part of the settings backup).
        val weatherJson = stringPreferencesKey("weather_json")
        val weatherAt = longPreferencesKey("weather_at")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { read(it) }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(change: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val s = change(read(p))
            p[K.language] = s.language
            p[K.cityId] = s.cityId
            p[K.customName] = s.customName
            p[K.lat] = s.lat
            p[K.lon] = s.lon
            p[K.zone] = s.zoneId
            p[K.method] = s.prayerMethod.name
            p[K.offsets] = s.offsets.joinToString(",")
            p[K.weatherEnabled] = s.weatherEnabled
            p[K.weatherMinutes] = s.weatherMinutes
            p[K.wallpaperMinutes] = s.wallpaperMinutes
            p[K.pinned] = s.pinnedWallpaper
            p[K.accent] = s.accent.name
            p[K.screensaver] = s.screensaverMinutes
            p[K.pin] = s.pinHash
            p[K.order] = s.appOrder.joinToString(",")
            p[K.favorites] = s.favorites.joinToString(",")
            p[K.hidden] = s.hidden.joinToString(",")
            p[K.entertainment] = s.entertainment.joinToString(",")
            p[K.notEntertainment] = s.notEntertainment.joinToString(",")
            p[K.safeArea] = s.tvSafeArea
            p[K.use24h] = s.use24h
        }
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            language = p[K.language] ?: d.language,
            cityId = p[K.cityId] ?: d.cityId,
            customName = p[K.customName] ?: d.customName,
            lat = p[K.lat] ?: d.lat,
            lon = p[K.lon] ?: d.lon,
            zoneId = p[K.zone] ?: d.zoneId,
            prayerMethod = p[K.method]?.let { runCatching { PrayerMethodId.valueOf(it) }.getOrNull() } ?: d.prayerMethod,
            offsets = p[K.offsets]?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 6 } ?: d.offsets,
            weatherEnabled = p[K.weatherEnabled] ?: d.weatherEnabled,
            weatherMinutes = p[K.weatherMinutes] ?: d.weatherMinutes,
            wallpaperMinutes = p[K.wallpaperMinutes] ?: d.wallpaperMinutes,
            pinnedWallpaper = p[K.pinned] ?: d.pinnedWallpaper,
            accent = p[K.accent]?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: d.accent,
            screensaverMinutes = p[K.screensaver] ?: d.screensaverMinutes,
            pinHash = p[K.pin] ?: d.pinHash,
            appOrder = p[K.order]?.split(",")?.filter { it.isNotBlank() } ?: d.appOrder,
            favorites = p[K.favorites]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: d.favorites,
            hidden = p[K.hidden]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: d.hidden,
            entertainment = p[K.entertainment]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: d.entertainment,
            notEntertainment = p[K.notEntertainment]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: d.notEntertainment,
            tvSafeArea = p[K.safeArea] ?: d.tvSafeArea,
            use24h = p[K.use24h] ?: d.use24h,
        )
    }

    // ---- weather cache -------------------------------------------------------------------------------

    val weatherCache: Flow<Pair<String?, Long>> = context.dataStore.data.map { it[K.weatherJson] to (it[K.weatherAt] ?: 0L) }

    suspend fun saveWeather(json: String, at: Long) {
        context.dataStore.edit { it[K.weatherJson] = json; it[K.weatherAt] = at }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    // ---- backup --------------------------------------------------------------------------------------

    fun toJson(s: AppSettings): String = JSONObject().apply {
        put("format", "qatar4u-tv-settings/1")
        put("language", s.language); put("cityId", s.cityId); put("customName", s.customName)
        put("lat", s.lat); put("lon", s.lon); put("zoneId", s.zoneId)
        put("prayerMethod", s.prayerMethod.name); put("offsets", s.offsets.joinToString(","))
        put("weatherEnabled", s.weatherEnabled); put("weatherMinutes", s.weatherMinutes)
        put("wallpaperMinutes", s.wallpaperMinutes); put("pinnedWallpaper", s.pinnedWallpaper)
        put("accent", s.accent.name); put("screensaverMinutes", s.screensaverMinutes)
        put("appOrder", s.appOrder.joinToString(",")); put("favorites", s.favorites.joinToString(","))
        put("hidden", s.hidden.joinToString(","))
        put("entertainment", s.entertainment.joinToString(",")); put("notEntertainment", s.notEntertainment.joinToString(","))
        put("tvSafeArea", s.tvSafeArea); put("use24h", s.use24h)
        // The PIN is deliberately not exported.
    }.toString(2)

    fun fromJson(text: String, keepPin: String): AppSettings {
        val o = JSONObject(text)
        require(o.optString("format").startsWith("qatar4u-tv-settings/")) { "not a Qatar4u TV backup" }
        val d = AppSettings()
        fun list(k: String) = o.optString(k).split(",").filter { it.isNotBlank() }
        return AppSettings(
            language = o.optString("language", d.language),
            cityId = o.optString("cityId", d.cityId),
            customName = o.optString("customName", d.customName),
            lat = o.optDouble("lat", d.lat).coerceIn(-90.0, 90.0),
            lon = o.optDouble("lon", d.lon).coerceIn(-180.0, 180.0),
            zoneId = o.optString("zoneId", d.zoneId),
            prayerMethod = runCatching { PrayerMethodId.valueOf(o.optString("prayerMethod")) }.getOrDefault(d.prayerMethod),
            offsets = list("offsets").mapNotNull { it.toIntOrNull()?.coerceIn(-30, 30) }.takeIf { it.size == 6 } ?: d.offsets,
            weatherEnabled = o.optBoolean("weatherEnabled", d.weatherEnabled),
            weatherMinutes = o.optInt("weatherMinutes", d.weatherMinutes).coerceIn(30, 360),
            wallpaperMinutes = o.optInt("wallpaperMinutes", d.wallpaperMinutes).coerceIn(0, 1440),
            pinnedWallpaper = o.optString("pinnedWallpaper", ""),
            accent = runCatching { Accent.valueOf(o.optString("accent")) }.getOrDefault(d.accent),
            screensaverMinutes = o.optInt("screensaverMinutes", d.screensaverMinutes).coerceIn(0, 120),
            pinHash = keepPin,
            appOrder = list("appOrder"),
            favorites = list("favorites").toSet(),
            hidden = list("hidden").toSet(),
            entertainment = list("entertainment").toSet(),
            notEntertainment = list("notEntertainment").toSet(),
            tvSafeArea = o.optInt("tvSafeArea", 0).coerceIn(0, 10),
            use24h = o.optBoolean("use24h", false),
        )
    }
}
