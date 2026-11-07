package com.taurotechnologies.godseye.nativeapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.taurotechnologies.godseye.nativeapp.data.FlightMotion
import com.taurotechnologies.godseye.nativeapp.data.RadarSignal
import com.taurotechnologies.godseye.nativeapp.data.SatelliteMotion
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * The signal's location line, live for moving craft. Flights and satellites count forward every second
 * along the same motion models that move their map markers, so the card matches the icon on the map.
 * Everything else returns its fixed [RadarSignal.locationName]. Shared by every card that shows a signal.
 */
@Composable
fun liveLocationName(signal: RadarSignal): String {
    val flight = signal.flight
    val satellite = signal.satellite
    if (flight == null && satellite == null) return signal.locationName
    var now by remember(signal.id) { mutableDoubleStateOf(System.currentTimeMillis() / 1000.0) }
    LaunchedEffect(signal.id) {
        while (true) {
            delay(LIVE_LOCATION_TICK_MS)
            now = System.currentTimeMillis() / 1000.0
        }
    }
    return if (flight != null) {
        val p = FlightMotion.position(flight, now)
        formatLatLng(p.lat, p.lng)
    } else {
        val p = SatelliteMotion.position(satellite!!, now)
        "${signal.locationName} · ${formatLatLng(p.lat, p.lng)}"
    }
}

/** Three decimals (~100 m), so a moving aircraft's numbers visibly change every second. */
internal fun formatLatLng(lat: Double, lng: Double): String = String.format(Locale.ROOT, "%.3f°, %.3f°", lat, lng)

private const val LIVE_LOCATION_TICK_MS = 1_000L
