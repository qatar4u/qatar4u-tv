package net.qatar4u.tv

import android.app.Application
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.qatar4u.tv.data.SettingsStore
import net.qatar4u.tv.weather.Weather

class Q4uApp : Application(), Configuration.Provider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.WARN).build()

    override fun onCreate() {
        super.onCreate()
        // Keep the background weather job in step with the owner's settings.
        scope.launch {
            SettingsStore(this@Q4uApp).settings
                .map { it.weatherEnabled to it.weatherMinutes }
                .distinctUntilChanged()
                .collect { (on, minutes) -> Weather.schedule(this@Q4uApp, minutes, on) }
        }
    }
}
