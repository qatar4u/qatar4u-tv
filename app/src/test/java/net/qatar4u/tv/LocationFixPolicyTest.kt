package net.qatar4u.tv

import net.qatar4u.tv.location.LocationFixPolicy as Policy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFixPolicyTest {
    @Test fun acceptsRecentCoarseFix() {
        assertTrue(Policy.usable(25.2854, 51.5310, 2000f, 30_000))
    }
    @Test fun rejectsOldAndFutureFixes() {
        assertFalse(Policy.usable(25.0, 51.0, 100f, Policy.MAX_AGE_MS + 1))
        assertFalse(Policy.usable(25.0, 51.0, 100f, -1))
        assertTrue(Policy.usable(25.0, 51.0, 100f, Policy.MAX_AGE_MS))
    }
    @Test fun rejectsInvalidCoordinates() {
        for (lat in listOf(Double.NaN, Double.POSITIVE_INFINITY, 91.0))
            assertFalse(Policy.usable(lat, 51.0, 100f, 0))
        assertFalse(Policy.usable(25.0, -181.0, 100f, 0))
    }
    @Test fun rejectsUnknownOrExcessiveAccuracy() {
        for (accuracy in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f, 5001f))
            assertFalse(Policy.usable(25.0, 51.0, accuracy, 0))
        assertTrue(Policy.usable(25.0, 51.0, 5000f, 0))
    }
}
