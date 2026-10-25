package net.qatar4u.tv.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object CurrentLocation {
    private fun usable(location: Location): Boolean = location.hasAccuracy() && LocationFixPolicy.usable(
        location.latitude, location.longitude, location.accuracy,
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L,
    )

    /** Foreground, one-shot request only; coarse permission is enough for a city-level network fix. */
    @SuppressLint("MissingPermission")
    suspend fun find(context: Context): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        val cached = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.filter(::usable).maxByOrNull { it.elapsedRealtimeNanos }
        if (cached != null) return cached
        if (LocationManager.NETWORK_PROVIDER !in providers) return null
        return withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine { continuation ->
                val cancellation = CancellationSignal()
                continuation.invokeOnCancellation { cancellation.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(
                        manager, LocationManager.NETWORK_PROVIDER, cancellation,
                        ContextCompat.getMainExecutor(context),
                    ) { location ->
                        if (continuation.isActive) continuation.resume(location?.takeIf(::usable))
                    }
                } catch (_: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                } catch (_: IllegalArgumentException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }
}
