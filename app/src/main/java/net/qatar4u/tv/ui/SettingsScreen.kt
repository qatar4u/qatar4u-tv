package net.qatar4u.tv.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qatar4u.tv.BuildConfigInfo
import net.qatar4u.tv.R
import net.qatar4u.tv.apps.AppEntry
import net.qatar4u.tv.apps.Apps
import net.qatar4u.tv.data.Accent
import net.qatar4u.tv.data.AppSettings
import net.qatar4u.tv.data.Cities
import net.qatar4u.tv.data.PrayerMethodId
import net.qatar4u.tv.prayer.PrayerMethod
import net.qatar4u.tv.prayer.PrayerTimes
import net.qatar4u.tv.util.Dex
import net.qatar4u.tv.util.Pin
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.ZoneId

enum class Section(val title: Int) {
    GENERAL(R.string.s_general), LOCATION(R.string.s_location), PRAYER(R.string.s_prayer), WEATHER(R.string.s_weather),
    WALLPAPERS(R.string.s_wallpapers), APPS(R.string.s_apps), LOOK(R.string.s_look), SCREENSAVER(R.string.s_screensaver),
    REMOTE(R.string.s_remote), DISPLAY(R.string.s_display), BACKUP(R.string.s_backup), OPTIMIZE(R.string.s_optimize),
    DOWNLOADS(R.string.s_downloads), SECURITY(R.string.s_security), ABOUT(R.string.s_about),
}

/** What the settings screen needs from the activity: pickers and permissions live there. */
class SettingsActions(
    val onBack: () -> Unit,
    val onLanguage: (String) -> Unit,
    val onUseLocation: () -> Unit,
    val onAddWallpapers: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onClearData: () -> Unit,
    val onPreviewSaver: () -> Unit,
    val onToast: (String) -> Unit,
)

@Composable
fun SettingsScreen(
    s: AppSettings,
    allApps: List<AppEntry>,
    wallpapers: List<Wallpapers.Item>,
    lastKey: String,
    wallpaperBitmap: android.graphics.Bitmap?,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    a: SettingsActions,
) {
    var section by remember { mutableStateOf(Section.GENERAL) }
    val first = remember { FocusRequester() }
    Box(Modifier.fillMaxSize()) {
        WallpaperLayer(wallpaperBitmap, dim = 0.55f)
        Row(Modifier.fillMaxSize().padding((24 + s.tvSafeArea * 8).dp)) {
            LazyColumn(
                Modifier.width(280.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(6.dp),
            ) {
                item {
                    Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, color = Q4u.TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                item { FocusButton(stringResource(R.string.back), Modifier.fillMaxWidth(), onClick = a.onBack) }
                items(Section.entries.toList()) { sec ->
                    FocusButton(
                        stringResource(sec.title),
                        Modifier.fillMaxWidth().then(if (sec == Section.GENERAL) Modifier.focusRequester(first) else Modifier),
                        selected = sec == section,
                        // Moving down the list previews each section, as TV settings menus do.
                        onFocus = { if (it) section = sec },
                        onClick = { section = sec },
                    )
                }
            }
            Spacer(Modifier.width(24.dp))
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(Q4u.Background.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(6.dp)) {
                    item { Text(stringResource(section.title), style = MaterialTheme.typography.headlineMedium, color = LocalAccent.current) }
                    item {
                        when (section) {
                            Section.GENERAL -> General(s, onChange, a)
                            Section.LOCATION -> Location(s, onChange, a)
                            Section.PRAYER -> Prayer(s, onChange)
                            Section.WEATHER -> WeatherSettings(s, onChange)
                            Section.WALLPAPERS -> WallpaperSettings(s, wallpapers, onChange, a)
                            Section.APPS -> AppsSettings(s, allApps, onChange)
                            Section.LOOK -> Look(s, onChange)
                            Section.SCREENSAVER -> Saver(s, onChange, a)
                            Section.REMOTE -> Remote(lastKey)
                            Section.DISPLAY -> Display(s, onChange)
                            Section.BACKUP -> Backup(a)
                            Section.OPTIMIZE -> Optimize()
                            Section.SECURITY -> Security(s, onChange, a)
                            Section.DOWNLOADS -> DownloaderScreen(a.onToast)
                            Section.ABOUT -> About()
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, color = Q4u.TextPrimary)

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = Q4u.TextSecondary)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(options: List<T>, selected: T, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { o -> FocusButton(label(o), selected = o == selected, onClick = { onPick(o) }) }
    }
}

@Composable
private fun General(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, a: SettingsActions) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.language))
        Choices(listOf("ar", "en"), s.language, { if (it == "ar") "العربية" else "English" }) { a.onLanguage(it) }
        Label(stringResource(R.string.clock_24h))
        Choices(listOf(false, true), s.use24h, { if (it) "24" else "12" }) { v -> onChange { it.copy(use24h = v) } }
        FocusButton(stringResource(R.string.home_default), onClick = {
            Apps.start(context, Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
    }
}

@Composable
private fun Location(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, a: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusButton(stringResource(R.string.use_location), selected = s.cityId == "custom", onClick = a.onUseLocation)
        if (s.cityId == "custom") Note("%.4f, %.4f · %s".format(s.lat, s.lon, s.zoneId))
        Choices(Cities.all, Cities.byId(s.cityId) ?: Cities.DOHA, { it.name(s.language) }) { c ->
            onChange { it.copy(cityId = c.id, lat = c.lat, lon = c.lon, zoneId = c.zone, customName = "") }
        }
    }
}

@Composable
private fun Prayer(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    val names = prayerNames()
    val zone = runCatching { ZoneId.of(s.zoneId) }.getOrDefault(ZoneId.of("Asia/Qatar"))
    val today = LocalDate.now(zone)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.method))
        Choices(PrayerMethodId.entries.toList(), s.prayerMethod,
            { stringResource(if (it == PrayerMethodId.QATAR) R.string.method_qatar else R.string.method_uaq) }) { m ->
            onChange { it.copy(prayerMethod = m) }
        }
        // Both methods side by side for today, so the owner can compare with the local calendar.
        val uaq = PrayerTimes.minutes(today, s.lat, s.lon, zone, PrayerMethod.UMM_AL_QURA)
        val qa = PrayerTimes.minutes(today, s.lat, s.lon, zone, PrayerMethod.QATAR)
        Note((0 until 6).joinToString("   ") { "${names[it]} ${PrayerTimes.clock(uaq[it], s.use24h)}/${PrayerTimes.clock(qa[it], s.use24h)}" })
        Label(stringResource(R.string.offsets))
        for (i in 0 until 6) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(names[i], color = Q4u.TextPrimary, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodyLarge)
                FocusButton("−1", onClick = { onChange { st -> st.copy(offsets = st.offsets.toMutableList().also { o -> o[i] = (o[i] - 1).coerceAtLeast(-30) }) } })
                Text("%+d".format(s.offsets[i]), color = LocalAccent.current, modifier = Modifier.width(56.dp), style = MaterialTheme.typography.titleMedium)
                FocusButton("+1", onClick = { onChange { st -> st.copy(offsets = st.offsets.toMutableList().also { o -> o[i] = (o[i] + 1).coerceAtMost(30) }) } })
            }
        }
        Note(stringResource(R.string.prayer_note))
    }
}

@Composable
private fun WeatherSettings(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.weather_on))
        Choices(listOf(true, false), s.weatherEnabled, { if (it) "✓" else "✕" }) { v -> onChange { it.copy(weatherEnabled = v) } }
        Label(stringResource(R.string.weather_every))
        Choices(listOf(30, 60, 120, 180), s.weatherMinutes, { stringResource(R.string.minutes_n, it) }) { v -> onChange { it.copy(weatherMinutes = v) } }
        Note(stringResource(R.string.weather_source))
    }
}

@Composable
private fun WallpaperSettings(s: AppSettings, items: List<Wallpapers.Item>, onChange: ((AppSettings) -> AppSettings) -> Unit, a: SettingsActions) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.wall_every))
        Choices(listOf(0, 15, 30, 60, 180), s.wallpaperMinutes,
            { if (it == 0) stringResource(R.string.wall_off) else stringResource(R.string.minutes_n, it) }) { v -> onChange { it.copy(wallpaperMinutes = v) } }
        if (s.pinnedWallpaper.isNotEmpty()) FocusButton(stringResource(R.string.wall_unpin), onClick = { onChange { it.copy(pinnedWallpaper = "") } })
        FocusButton(stringResource(R.string.wall_add), onClick = a.onAddWallpapers)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(8.dp)) {
            items(items, key = { it.name }) { item ->
                val thumb by produceState<android.graphics.Bitmap?>(null, item.name) { value = Wallpapers.load(context, item, 320, 180) }
                FocusCard(
                    Modifier.width(240.dp).height(135.dp),
                    onClick = { onChange { it.copy(pinnedWallpaper = item.name) } },
                ) { _ ->
                    thumb?.let { Image(it.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    if (item.name == s.pinnedWallpaper) Text("📌", modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
            }
        }
        Note(stringResource(R.string.wall_pin))
    }
}

@Composable
private fun AppsSettings(s: AppSettings, apps: List<AppEntry>, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Apps.ordered(apps, s.appOrder).forEach { app ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(app.label, color = if (app.pkg in s.hidden) Q4u.TextSecondary else Q4u.TextPrimary,
                    modifier = Modifier.width(220.dp), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                FocusButton(if (app.pkg in s.favorites) "★" else "☆", selected = app.pkg in s.favorites, onClick = {
                    onChange { st -> st.copy(favorites = if (app.pkg in st.favorites) st.favorites - app.pkg else st.favorites + app.pkg) }
                })
                FocusButton(stringResource(if (app.pkg in s.hidden) R.string.menu_unhide else R.string.menu_hide), onClick = {
                    onChange { st -> st.copy(hidden = if (app.pkg in st.hidden) st.hidden - app.pkg else st.hidden + app.pkg) }
                })
                FocusButton("▲", onClick = { onChange { st -> st.copy(appOrder = Apps.move(Apps.ordered(apps, st.appOrder).map { it.pkg }, app.pkg, -1)) } })
                FocusButton("▼", onClick = { onChange { st -> st.copy(appOrder = Apps.move(Apps.ordered(apps, st.appOrder).map { it.pkg }, app.pkg, 1)) } })
            }
        }
    }
}

@Composable
private fun Look(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.accent))
        Choices(Accent.entries.toList(), s.accent, {
            stringResource(when (it) { Accent.GOLD -> R.string.accent_gold; Accent.MAROON -> R.string.accent_maroon; Accent.PEARL -> R.string.accent_pearl })
        }) { v -> onChange { it.copy(accent = v) } }
        var transparency by remember(s.appCardOpacityPercent) {
            mutableFloatStateOf((100 - s.appCardOpacityPercent).toFloat())
        }
        Label(stringResource(R.string.card_transparency, transparency.roundToInt()))
        Slider(
            value = transparency,
            onValueChange = { transparency = it },
            onValueChangeFinished = {
                val opacity = 100 - transparency.roundToInt()
                onChange { it.copy(appCardOpacityPercent = opacity) }
            },
            valueRange = 0f..100f,
            steps = 19,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FocusButton(stringResource(R.string.card_less_transparent), onClick = {
                onChange { it.copy(appCardOpacityPercent = (it.appCardOpacityPercent + 5).coerceAtMost(100)) }
            })
            FocusButton(stringResource(R.string.card_more_transparent), onClick = {
                onChange { it.copy(appCardOpacityPercent = (it.appCardOpacityPercent - 5).coerceAtLeast(0)) }
            })
        }
        // Only the surface uses alpha: labels, icons and the focus outline keep their contrast.
        FocusCard(
            Modifier.width(224.dp).height(126.dp),
            background = Q4u.Surface.copy(alpha = 1f - transparency / 100f),
            onClick = {},
        ) { Text(stringResource(R.string.card_preview), color = Q4u.TextPrimary) }
        Note(stringResource(R.string.card_transparency_note))
        FocusButton(stringResource(R.string.card_reset), onClick = {
            onChange { it.copy(appCardOpacityPercent = 82) }
        })
    }
}

@Composable
private fun Saver(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, a: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.saver_after))
        Choices(listOf(0, 5, 10, 20, 30, 60), s.screensaverMinutes,
            { if (it == 0) stringResource(R.string.wall_off) else stringResource(R.string.minutes_n, it) }) { v -> onChange { it.copy(screensaverMinutes = v) } }
        FocusButton(stringResource(R.string.saver_test), onClick = a.onPreviewSaver)
    }
}

@Composable
private fun Remote(lastKey: String) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Note(stringResource(R.string.remote_help))
        Text(stringResource(R.string.remote_last, lastKey.ifEmpty { "—" }), style = MaterialTheme.typography.headlineMedium, color = LocalAccent.current)
        Note(stringResource(R.string.remote_cec))
        FocusButton(stringResource(R.string.open_bluetooth), onClick = {
            Apps.start(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
    }
}

@Composable
private fun Display(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    val context = LocalContext.current
    val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val cfg = context.resources.configuration
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.safe_area))
        Choices((0..5).toList(), s.tvSafeArea, { "$it" }) { v -> onChange { it.copy(tvSafeArea = v) } }
        Label(stringResource(R.string.display_info))
        dm.displays.forEach { d ->
            val m = android.util.DisplayMetrics().also { d.getRealMetrics(it) }
            Note("#${d.displayId}  ${d.name}  ${m.widthPixels}×${m.heightPixels}  ${m.densityDpi}dpi")
        }
        Note("${cfg.screenWidthDp}×${cfg.screenHeightDp} dp")
        Text(stringResource(if (Dex.isOn(context)) R.string.dex_on else R.string.dex_off), color = LocalAccent.current, style = MaterialTheme.typography.titleMedium)
        Note(stringResource(R.string.dex_note))
    }
}

@Composable
private fun Backup(a: SettingsActions) {
    var confirm by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusButton(stringResource(R.string.backup_export), onClick = a.onExport)
        FocusButton(stringResource(R.string.backup_import), onClick = a.onImport)
        FocusButton(stringResource(if (confirm) R.string.clear_confirm else R.string.clear_data), onClick = {
            if (confirm) a.onClearData() else confirm = true
        })
    }
}

@Composable
private fun Optimize() {
    val context = LocalContext.current
    val disabled by produceState(emptyList<String>()) { value = withContext(Dispatchers.IO) { Apps.disabledPackages(context) } }
    val user by produceState(emptyList<AppEntry>()) { value = withContext(Dispatchers.IO) { Apps.installed(context).filter { !it.isSystem } } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Note(stringResource(R.string.opt_intro))
        Label(stringResource(R.string.opt_disabled, disabled.size))
        disabled.forEach { pkg -> FocusButton(pkg, onClick = { Apps.openAppInfo(context, pkg) }) }
        Label(stringResource(R.string.opt_user_apps, user.size))
        user.forEach { app -> FocusButton("${app.label}  ·  ${app.pkg}", onClick = { Apps.openAppInfo(context, app.pkg) }) }
    }
}

@Composable
private fun Security(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, a: SettingsActions) {
    var entry by remember { mutableStateOf("") }
    val done = stringResource(R.string.backup_done)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (s.pinHash.isNotEmpty()) {
            FocusButton(stringResource(R.string.pin_remove), onClick = { onChange { it.copy(pinHash = "") }; a.onToast(done) })
        } else {
            Label(stringResource(R.string.pin_new))
            PinPad(entry, onDigit = { if (entry.length < 4) entry += it }, onClear = { entry = "" }, onOk = {
                if (entry.length == 4) { val h = Pin.hash(entry); onChange { it.copy(pinHash = h) }; entry = ""; a.onToast(done) }
            })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinPad(entry: String, onDigit: (String) -> Unit, onClear: () -> Unit, onOk: () -> Unit, firstFocus: FocusRequester? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("●".repeat(entry.length) + "○".repeat(4 - entry.length), style = MaterialTheme.typography.headlineMedium, color = LocalAccent.current)
        for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓"))) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { k ->
                    FocusButton(
                        k,
                        Modifier.width(76.dp).then(if (k == "1" && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier),
                        onClick = { when (k) { "⌫" -> onClear(); "✓" -> onOk(); else -> onDigit(k) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun About() {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Note(stringResource(R.string.version, BuildConfigInfo.versionName(context)))
        Note("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Note("${Build.DISPLAY} · ${Build.VERSION.SECURITY_PATCH}")
        Note("Wallpapers: original illustrations by Qatar4u and photos supplied by the owner.")
        Note("Weather: Open-Meteo.com, CC BY 4.0. Font: Cairo, SIL Open Font License.")
    }
}

fun copyToClipboard(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Qatar4u TV", text))
}
