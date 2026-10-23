package net.qatar4u.tv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qatar4u.tv.apps.AppEntry
import net.qatar4u.tv.apps.Apps
import net.qatar4u.tv.data.AppSettings
import net.qatar4u.tv.data.SettingsStore
import net.qatar4u.tv.weather.Weather
import net.qatar4u.tv.weather.WeatherNow

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val store = SettingsStore(app)

    val settings: StateFlow<AppSettings?> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val weather: StateFlow<WeatherNow?> = store.weatherCache
        .map { (json, at) -> json?.let { Weather.parse(it, at) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _now = MutableStateFlow(System.currentTimeMillis())
    /** Ticks at the start of each minute, so clocks and countdowns stay in step without busy redraws. */
    val now: StateFlow<Long> = _now

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    /** Apps in the owner's order with hidden ones removed. */
    val visibleApps: StateFlow<List<AppEntry>> = combine(_apps, store.settings) { list, s ->
        Apps.ordered(list, s.appOrder).filter { it.pkg !in s.hidden }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                val t = System.currentTimeMillis()
                _now.value = t
                delay(60_000 - t % 60_000 + 50)
            }
        }
        reloadApps()
        viewModelScope.launch {
            // Refresh on start when the cache is older than the owner's interval.
            val s = store.current()
            val (_, at) = store.weatherCache.first()
            if (s.weatherEnabled && System.currentTimeMillis() - at > s.weatherMinutes * 60_000L) refreshWeather()
        }
    }

    fun reloadApps() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { Apps.installed(getApplication()) }
        }
    }

    fun refreshWeather() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            Weather.refresh(getApplication())
            _refreshing.value = false
        }
    }

    fun update(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { store.update(change) }
    }
}
