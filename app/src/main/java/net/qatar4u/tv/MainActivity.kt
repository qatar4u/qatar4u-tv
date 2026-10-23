package net.qatar4u.tv

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.qatar4u.tv.data.AppSettings
import net.qatar4u.tv.ui.HomeScreen
import net.qatar4u.tv.ui.PinPad
import net.qatar4u.tv.ui.Q4u
import net.qatar4u.tv.ui.Q4uTheme
import net.qatar4u.tv.ui.Screensaver
import net.qatar4u.tv.ui.SettingsActions
import net.qatar4u.tv.ui.SettingsScreen
import net.qatar4u.tv.ui.Wallpapers
import net.qatar4u.tv.ui.color
import net.qatar4u.tv.util.Lang
import net.qatar4u.tv.util.Pin
import java.util.TimeZone

private enum class Screen { HOME, PIN, SETTINGS }

class MainActivity : ComponentActivity() {

    private val lastInput = mutableLongStateOf(System.currentTimeMillis())
    private val lastKey = mutableStateOf("")
    private val saverOn = mutableStateOf(false)
    private val libraryAllowed = mutableStateOf(false)
    private val wallpaperVersion = mutableStateOf(0)
    private val homePressed = mutableStateOf(0)
    private var vm: MainViewModel? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Lang.wrap(newBase))
    }

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) applyLocation() else toast(getString(R.string.location_denied))
    }

    private val askVideos = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        libraryAllowed.value = ok
    }

    private val pickWallpapers = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) lifecycleScope.launch {
            Wallpapers.import(this@MainActivity, uris)
            wallpaperVersion.value++
        }
    }

    private val exportSettings = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val m = vm ?: return@registerForActivityResult
        if (uri != null) lifecycleScope.launch {
            val ok = runCatching {
                val json = m.store.toJson(m.store.current())
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }.isSuccess
            toast(getString(if (ok) R.string.backup_done else R.string.backup_failed))
        }
    }

    private val importSettings = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val m = vm ?: return@registerForActivityResult
        if (uri != null) lifecycleScope.launch {
            val ok = runCatching {
                val text = contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                val keepPin = m.store.current().pinHash
                val restored = m.store.fromJson(text, keepPin)
                m.store.update { restored }
                Lang.set(this@MainActivity, restored.language)
            }.isSuccess
            toast(getString(if (ok) R.string.backup_done else R.string.backup_failed))
            if (ok) recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        libraryAllowed.value = hasVideoPermission()
        setContent {
            val model: MainViewModel = viewModel()
            vm = model
            App(model)
        }
    }

    override fun onResume() {
        super.onResume()
        vm?.reloadApps()
        libraryAllowed.value = hasVideoPermission()
        lastInput.longValue = System.currentTimeMillis()
    }

    @Composable
    private fun App(model: MainViewModel) {
        val s = model.settings.collectAsState().value ?: return
        val now by model.now.collectAsState()
        val weather by model.weather.collectAsState()
        val refreshing by model.refreshing.collectAsState()
        val offline by model.offline.collectAsState()
        val apps by model.visibleApps.collectAsState()
        val allApps by model.apps.collectAsState()
        var screen by remember { mutableStateOf(Screen.HOME) }
        LaunchedEffect(homePressed.value) { if (homePressed.value > 0) screen = Screen.HOME }

        val wallItems = remember(wallpaperVersion.value) { Wallpapers.list(this) }
        val index = Wallpapers.pick(wallItems, s.pinnedWallpaper, s.wallpaperMinutes, now)
        val config = LocalConfiguration.current
        val density = LocalDensity.current
        val wPx = with(density) { config.screenWidthDp.dp.roundToPx() }
        val hPx = with(density) { config.screenHeightDp.dp.roundToPx() }
        val wallpaper by produceState<Bitmap?>(null, index, wallItems, wPx, hPx) {
            value = wallItems.getOrNull(index)?.let { Wallpapers.load(this@MainActivity, it, wPx, hPx) }
        }

        // Screensaver after the owner's idle time; any key or touch ends it (see dispatch* below).
        LaunchedEffect(s.screensaverMinutes) {
            while (true) {
                delay(15_000)
                val idle = System.currentTimeMillis() - lastInput.longValue
                if (s.screensaverMinutes > 0 && idle > s.screensaverMinutes * 60_000L && screen == Screen.HOME) saverOn.value = true
            }
        }

        Q4uTheme(s.accent.color()) {
            BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
            when (screen) {
                Screen.HOME -> HomeScreen(
                    s = s, now = now, weather = weather, refreshing = refreshing, offline = offline, apps = apps, wallpaper = wallpaper,
                    libraryAllowed = libraryAllowed.value,
                    onAskLibrary = { askVideos.launch(videoPermission()) },
                    onRefreshWeather = { model.refreshWeather() },
                    onOpenSettings = { screen = if (s.pinHash.isNotEmpty()) Screen.PIN else Screen.SETTINGS },
                    onChange = model::update,
                    onToast = ::toast,
                )
                Screen.PIN -> PinGate(s, onOk = { screen = Screen.SETTINGS })
                Screen.SETTINGS -> SettingsScreen(
                    s = s, allApps = allApps, wallpapers = wallItems, lastKey = lastKey.value, wallpaperBitmap = wallpaper,
                    onChange = model::update,
                    a = SettingsActions(
                        onBack = { screen = Screen.HOME },
                        onLanguage = { lang ->
                            Lang.set(this, lang)
                            lifecycleScope.launch { model.store.update { it.copy(language = lang) }; recreate() }
                        },
                        onUseLocation = {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) applyLocation()
                            else askLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                        onAddWallpapers = { pickWallpapers.launch(arrayOf("image/*")) },
                        onExport = { exportSettings.launch("qatar4u-tv-settings.json") },
                        onImport = { importSettings.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onClearData = {
                            lifecycleScope.launch {
                                model.store.clearAll()
                                getSharedPreferences("lang", MODE_PRIVATE).edit().clear().apply()
                                filesDir.resolve("wallpapers").deleteRecursively()
                                recreate()
                            }
                        },
                        onPreviewSaver = { screen = Screen.HOME; saverOn.value = true },
                        onToast = ::toast,
                    ),
                )
            }
            if (saverOn.value) Screensaver(s, now, wallpaper)
        }
    }

    @Composable
    private fun PinGate(s: AppSettings, onOk: () -> Unit) {
        var entry by remember { mutableStateOf("") }
        var wrong by remember { mutableStateOf(false) }
        val first = remember { FocusRequester() }
        Box(Modifier.fillMaxSize().background(Q4u.Background), contentAlignment = Alignment.Center) {
            Column(
                Modifier.background(Q4u.Surface, RoundedCornerShape(24.dp)).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.pin_enter), style = MaterialTheme.typography.headlineMedium, color = Q4u.TextPrimary)
                if (wrong) Text(stringResource(R.string.pin_wrong), color = Q4u.Danger)
                PinPad(entry, onDigit = { if (entry.length < 4) entry += it; wrong = false }, onClear = { entry = "" }, onOk = {
                    if (Pin.check(entry, s.pinHash)) onOk() else { wrong = true; entry = "" }
                }, firstFocus = first)
            }
        }
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    }

    @SuppressLint("MissingPermission")
    private fun applyLocation() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val best: Location? = lm.getProviders(true).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (best == null) { toast(getString(R.string.location_none)); return }
        lifecycleScope.launch {
            vm?.store?.update {
                it.copy(cityId = "custom", lat = best.latitude, lon = best.longitude, zoneId = TimeZone.getDefault().id,
                    customName = getString(R.string.my_location))
            }
            vm?.refreshWeather()
        }
    }

    private fun videoPermission() =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasVideoPermission() =
        ContextCompat.checkSelfPermission(this, videoPermission()) == PackageManager.PERMISSION_GRANTED

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    // ---- input: idle tracking, screensaver exit, remote test ------------------------------------------

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lastInput.longValue = System.currentTimeMillis()
        if (event.action == KeyEvent.ACTION_DOWN) lastKey.value = KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_")
        if (saverOn.value) {
            if (event.action == KeyEvent.ACTION_UP) saverOn.value = false
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        lastInput.longValue = System.currentTimeMillis()
        if (saverOn.value) {
            if (ev.actionMasked == MotionEvent.ACTION_UP) saverOn.value = false
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        lastInput.longValue = System.currentTimeMillis()
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home pressed while Qatar4u TV is in front: wake up and go back to the home screen.
        saverOn.value = false
        homePressed.value++
    }
}
