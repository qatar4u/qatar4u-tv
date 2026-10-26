package net.qatar4u.tv.location

/** Reject stale or implausible fixes before using them for prayer times and weather. */
object LocationFixPolicy {
    const val MAX_AGE_MS = 5 * 60_000L
    const val MAX_ACCURACY_METERS = 5_000f

    fun usable(lat: Double, lon: Double, accuracy: Float, ageMs: Long): Boolean =
        lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0 &&
            accuracy.isFinite() && accuracy > 0f && accuracy <= MAX_ACCURACY_METERS &&
            ageMs in 0..MAX_AGE_MS
}
