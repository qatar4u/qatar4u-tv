package net.qatar4u.tv.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.qatar4u.tv.R
import net.qatar4u.tv.apps.AppEntry
import net.qatar4u.tv.apps.Apps
import net.qatar4u.tv.data.AppSettings
import net.qatar4u.tv.data.Cities
import net.qatar4u.tv.prayer.PrayerMethod
import net.qatar4u.tv.prayer.PrayerTimes
import net.qatar4u.tv.weather.Weather
import net.qatar4u.tv.weather.WeatherNow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HIJRI_AR = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
private val HIJRI_EN = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula", "Jumada al-Akhirah",
    "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah")

fun hijriText(date: LocalDate, zone: ZoneId, ar: Boolean): String = runCatching {
    val (d, m, y) = PrayerTimes.hijriParts(date, zone).let { Triple(it[0], it[1], it[2]) }
    if (ar) "$d ${HIJRI_AR[m]} $y هـ" else "$d ${HIJRI_EN[m]} $y AH"
}.getOrDefault("")

fun cityName(s: AppSettings): String {
    val ar = s.language == "ar"
    if (s.cityId == "custom") return s.customName.ifBlank { if (ar) "موقعي" else "My location" }
    return Cities.byId(s.cityId)?.name(s.language) ?: s.customName
}

/** Prayer names in display order, from string resources. */
@Composable
fun prayerNames() = listOf(
    stringResource(R.string.fajr), stringResource(R.string.sunrise), stringResource(R.string.dhuhr),
    stringResource(R.string.asr), stringResource(R.string.maghrib), stringResource(R.string.isha),
)

@Composable
fun durationText(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (h > 0) stringResource(R.string.hours_minutes, h, m) else stringResource(R.string.minutes_only, m)
}

@Composable
fun WallpaperLayer(bitmap: Bitmap?, dim: Float = 0f) {
    Box(Modifier.fillMaxSize().background(Q4u.Background)) {
        Crossfade(targetState = bitmap, animationSpec = tween(1200), label = "wallpaper") { b ->
            if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // Darker towards the rows so text stays readable on any picture.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.3f to Color.Black.copy(alpha = 0.15f),
                    0.55f to Color.Black.copy(alpha = 0.45f),
                    1f to Color.Black.copy(alpha = 0.92f),
                ),
            ),
        )
        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
    }
}

@Composable
fun HomeScreen(
    s: AppSettings,
    now: Long,
    weather: WeatherNow?,
    refreshing: Boolean,
    offline: Boolean,
    apps: List<AppEntry>,
    wallpaper: Bitmap?,
    libraryAllowed: Boolean,
    onAskLibrary: () -> Unit,
    onRefreshWeather: () -> Unit,
    onOpenSettings: () -> Unit,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val ar = s.language == "ar"
    val zone = runCatching { ZoneId.of(s.zoneId) }.getOrDefault(ZoneId.of("Asia/Qatar"))
    val zdt = Instant.ofEpochMilli(now).atZone(zone)
    val today = zdt.toLocalDate()
    val nowMin = zdt.hour * 60 + zdt.minute
    val method = PrayerMethod.of(s.prayerMethod)
    val times = remember(today, s.lat, s.lon, s.zoneId, s.prayerMethod, s.offsets) {
        PrayerTimes.minutes(today, s.lat, s.lon, zone, method, s.offsets)
    }
    val tomorrowFajr = remember(today, s.lat, s.lon, s.zoneId, s.prayerMethod, s.offsets) {
        PrayerTimes.minutes(today.plusDays(1), s.lat, s.lon, zone, method, s.offsets)[0]
    }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val firstFocus = remember { FocusRequester() }
    val failed = stringResource(R.string.open_failed)

    val favorites = apps.filter { it.pkg in s.favorites }
    val entertainment = apps.filter { it.isEntertainment && it.pkg !in s.favorites }
    val others = apps.filter { !it.isEntertainment && it.pkg !in s.favorites }
    val installed = apps.map { it.pkg }.toSet()
    val suggested = Apps.ENTERTAINMENT.filter { it.pkg !in installed && it.pkg !in s.hidden }

    Box(Modifier.fillMaxSize()) {
        WallpaperLayer(wallpaper)
        val edge = (24 + s.tvSafeArea * 8).dp
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = edge, end = edge, top = edge, bottom = edge + 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Header(s, zdt.toLocalDate(), nowMin, ar, zone, weather, refreshing, offline, onRefreshWeather, onOpenSettings)
            }
            item { PrayerStrip(times, nowMin, tomorrowFajr, s.use24h, method, ar) }

            val rows = buildList {
                if (favorites.isNotEmpty()) add(R.string.favorites to favorites)
                if (entertainment.isNotEmpty()) add(R.string.entertainment to entertainment)
                if (others.isNotEmpty()) add(R.string.all_apps to others)
            }
            rows.forEachIndexed { rowIndex, (title, list) ->
                item(key = "row-$title") {
                    AppRow(
                        title = stringResource(title),
                        apps = list,
                        favorites = s.favorites,
                        firstFocus = if (rowIndex == 0) firstFocus else null,
                        onOpen = { if (!Apps.launch(context, it)) onToast(failed) },
                        onMenu = { menuFor = it },
                    )
                }
            }
            if (suggested.isNotEmpty()) {
                item(key = "suggested") {
                    RowTitle(stringResource(R.string.suggested))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
                        items(suggested, key = { it.pkg }) { sug ->
                            FocusCard(Modifier.width(200.dp).height(96.dp), onClick = { Apps.openInPlay(context, sug.pkg) }) { f ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                                    Icon(Icons.Filled.Shop, null, tint = if (f) LocalAccent.current else Q4u.TextSecondary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(if (ar) sug.labelAr else sug.labelEn, color = Q4u.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "library") { LibraryRow(libraryAllowed, onAskLibrary, onToast) }
        }
    }

    LaunchedEffect(apps.isNotEmpty()) {
        // The first row is composed a frame or two after the app list arrives; try until it takes.
        if (apps.isNotEmpty()) repeat(10) {
            kotlinx.coroutines.delay(150)
            if (runCatching { firstFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    menuFor?.let { app ->
        AppMenu(
            app = app,
            isFavorite = app.pkg in s.favorites,
            onDismiss = { menuFor = null },
            onFavorite = {
                onChange { st -> st.copy(favorites = if (app.pkg in st.favorites) st.favorites - app.pkg else st.favorites + app.pkg) }
                menuFor = null
            },
            onMove = { delta ->
                val order = Apps.move(apps.map { it.pkg }, app.pkg, delta)
                onChange { st -> st.copy(appOrder = order) }
            },
            onHide = { onChange { st -> st.copy(hidden = st.hidden + app.pkg) }; menuFor = null },
            onInfo = { Apps.openAppInfo(context, app.pkg); menuFor = null },
            onUninstall = { Apps.uninstall(context, app.pkg); menuFor = null },
        )
    }
}

@Composable
private fun Header(
    s: AppSettings,
    date: LocalDate,
    nowMin: Int,
    ar: Boolean,
    zone: ZoneId,
    weather: WeatherNow?,
    refreshing: Boolean,
    offline: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    val accent = LocalAccent.current
    val locale = Locale(if (ar) "ar" else "en")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(PrayerTimes.clock(nowMin, s.use24h), style = MaterialTheme.typography.displayLarge, color = Q4u.TextPrimary)
                if (!s.use24h) Text(
                    if (nowMin < 720) (if (ar) " ص" else " AM") else (if (ar) " م" else " PM"),
                    style = MaterialTheme.typography.headlineMedium, color = Q4u.TextSecondary,
                    modifier = Modifier.padding(bottom = 18.dp),
                )
            }
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale)),
                style = MaterialTheme.typography.titleLarge, color = Q4u.TextPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(hijriText(date, zone, ar), style = MaterialTheme.typography.bodyLarge, color = accent)
                Text("  ·  " + cityName(s), style = MaterialTheme.typography.bodyLarge, color = Q4u.TextSecondary)
            }
        }
        if (s.weatherEnabled) {
            WeatherCard(weather, refreshing, offline, ar, onRefresh)
            Spacer(Modifier.width(16.dp))
        }
        FocusCard(Modifier.size(64.dp), shape = RoundedCornerShape(32.dp), onClick = onSettings) { f ->
            Icon(Icons.Filled.Settings, stringResource(R.string.settings), tint = if (f) accent else Q4u.TextPrimary, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun WeatherCard(w: WeatherNow?, refreshing: Boolean, offline: Boolean, ar: Boolean, onRefresh: () -> Unit) {
    val accent = LocalAccent.current
    FocusCard(Modifier.width(260.dp), onClick = onRefresh) { f ->
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            if (w == null) {
                Text(stringResource(R.string.weather_none), color = Q4u.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            } else {
                val stale = offline || System.currentTimeMillis() - w.fetchedAt > Weather.STALE_MS
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(weatherGlyph(w.code, w.isDay), fontSize = 34.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("${Math.round(w.temperatureC)}°", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = if (stale) Q4u.TextSecondary else Q4u.TextPrimary, fontFamily = Cairo)
                    Spacer(Modifier.width(10.dp))
                    Text(Weather.describe(w.code, ar), color = Q4u.TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    stringResource(R.string.weather_humidity, w.humidity) + " · " + stringResource(R.string.weather_wind, w.windKmh),
                    color = Q4u.TextSecondary, style = MaterialTheme.typography.bodyMedium,
                )
                val at = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(w.fetchedAt).atZone(ZoneId.systemDefault()))
                Text(
                    if (stale) stringResource(R.string.weather_offline, at) else stringResource(R.string.weather_last, at),
                    color = if (stale) Q4u.Danger else Q4u.TextSecondary, style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (f || refreshing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, null, tint = accent, modifier = Modifier.size(16.dp))
                    Text(" " + stringResource(R.string.refresh), color = accent, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun weatherGlyph(code: Int, day: Boolean): String = when (code) {
    0, 1 -> if (day) "☀️" else "🌙"
    2 -> if (day) "⛅" else "☁️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..67, in 80..82 -> "🌧️"
    in 71..77, 85, 86 -> "❄️"
    in 95..99 -> "⛈️"
    else -> "🌡️"
}

@Composable
private fun PrayerStrip(times: IntArray, nowMin: Int, tomorrowFajr: Int, use24h: Boolean, method: PrayerMethod, ar: Boolean) {
    val names = prayerNames()
    val next = PrayerTimes.next(times, nowMin)
    val left = PrayerTimes.minutesUntilNext(times, nowMin, tomorrowFajr)
    val accent = LocalAccent.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 0 until 6) {
                val isNext = i == next
                Column(
                    Modifier.weight(1f)
                        .background(if (isNext) accent.copy(alpha = 0.92f) else Q4u.Surface.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(names[i], color = if (isNext) Q4u.Background else Q4u.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    val shown = if (isNext && i == 0 && nowMin >= times[0]) tomorrowFajr else times[i]
                    Text(PrayerTimes.clock(shown, use24h), color = if (isNext) Q4u.Background else Q4u.TextPrimary, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.next_prayer, names[next], durationText(left)) + "   ·   " +
                stringResource(if (method.id == net.qatar4u.tv.data.PrayerMethodId.QATAR) R.string.method_qatar else R.string.method_uaq),
            color = Q4u.TextSecondary, style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun RowTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = Q4u.TextPrimary, modifier = Modifier.padding(start = 6.dp))
}

@Composable
private fun AppRow(
    title: String,
    apps: List<AppEntry>,
    favorites: Set<String>,
    firstFocus: FocusRequester?,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
) {
    Column {
        RowTitle(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
            items(apps.size, key = { apps[it].pkg }) { i ->
                val app = apps[i]
                AppCard(
                    app,
                    favorite = app.pkg in favorites,
                    modifier = if (i == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                    onClick = { onOpen(app) },
                    onLongClick = { onMenu(app) },
                )
            }
        }
    }
}

@Composable
private fun AppCard(app: AppEntry, favorite: Boolean, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val wPx = with(density) { 224.dp.roundToPx() }
    val hPx = with(density) { 126.dp.roundToPx() }
    val art by produceState<Bitmap?>(null, app.pkg) {
        value = withContext(Dispatchers.IO) {
            if (app.hasBanner) Apps.banner(context, app.pkg, wPx, hPx) else Apps.icon(context, app.pkg, with(density) { 72.dp.roundToPx() })
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var focused by remember { mutableStateOf(false) }
        FocusCard(
            modifier.width(224.dp).height(126.dp),
            onFocus = { focused = it },
            onLongClick = onLongClick,
            onClick = onClick,
        ) {
            val b = art
            if (b != null && app.hasBanner) {
                Image(b.asImageBitmap(), app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else if (b != null) {
                Image(b.asImageBitmap(), app.label, Modifier.size(72.dp))
            }
            if (favorite) Text("★", color = LocalAccent.current, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Text(
            app.label,
            color = if (focused) Q4u.TextPrimary else Q4u.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(224.dp).padding(top = 8.dp),
        )
    }
}

@Composable
private fun LibraryRow(allowed: Boolean, onAsk: () -> Unit, onToast: (String) -> Unit) {
    val context = LocalContext.current
    val videos by produceState(emptyList<Apps.Video>(), allowed) {
        value = if (allowed) withContext(Dispatchers.IO) { Apps.videos(context) } else emptyList()
    }
    val failed = stringResource(R.string.open_failed)
    Column {
        RowTitle(stringResource(R.string.library))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
            if (!allowed) {
                item {
                    FocusCard(Modifier.width(260.dp).height(96.dp), onClick = onAsk) { f ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Filled.VideoLibrary, null, tint = if (f) LocalAccent.current else Q4u.TextSecondary)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.library_allow), color = Q4u.TextPrimary, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else if (videos.isEmpty()) {
                item { Text(stringResource(R.string.library_empty), color = Q4u.TextSecondary, modifier = Modifier.padding(12.dp)) }
            } else {
                items(videos, key = { it.uri.toString() }) { v ->
                    FocusCard(Modifier.width(260.dp).height(96.dp), onClick = { if (!Apps.play(context, v)) onToast(failed) }) { _ ->
                        Column(Modifier.padding(14.dp).fillMaxWidth()) {
                            Text(v.title, color = Q4u.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            val m = (v.durationMs / 60000).toInt()
                            if (m > 0) Text(durationText(m), color = Q4u.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppMenu(
    app: AppEntry,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.background(Q4u.Surface, RoundedCornerShape(24.dp)).padding(24.dp).width(360.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(app.label, style = MaterialTheme.typography.headlineMedium, color = Q4u.TextPrimary)
            FocusButton(
                stringResource(if (isFavorite) R.string.menu_favorite_remove else R.string.menu_favorite_add),
                Modifier.fillMaxWidth().focusRequester(first), onClick = onFavorite,
            )
            FocusButton(stringResource(R.string.menu_move_left), Modifier.fillMaxWidth(), onClick = { onMove(-1) })
            FocusButton(stringResource(R.string.menu_move_right), Modifier.fillMaxWidth(), onClick = { onMove(1) })
            FocusButton(stringResource(R.string.menu_hide), Modifier.fillMaxWidth(), onClick = onHide)
            FocusButton(stringResource(R.string.menu_info), Modifier.fillMaxWidth(), onClick = onInfo)
            // Built-in system apps can't be removed, only disabled from App info.
            if (!app.isSystem) FocusButton(stringResource(R.string.menu_uninstall), Modifier.fillMaxWidth(), onClick = onUninstall)
            FocusButton(stringResource(R.string.close), Modifier.fillMaxWidth(), onClick = onDismiss)
        }
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    }
}
