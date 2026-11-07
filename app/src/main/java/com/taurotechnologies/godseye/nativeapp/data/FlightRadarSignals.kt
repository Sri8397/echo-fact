package com.taurotechnologies.godseye.nativeapp.data

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Native adapter for the simulated flight snapshot; no HTTP requests are made. */
fun mockFlightSignals(): List<RadarSignal> {
    val snapshot = MockFlightRepository.snapshot()
    val timestamp = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC)
    return snapshot.flights.map { flight ->
        RadarSignal(
            id = "mock-flight-${flight.id}",
            layer = RadarLayer.FLIGHTS,
            title = "Simulated flight ${flight.callsign ?: flight.id.uppercase(Locale.ROOT)}",
            locationName = when (flight.id) {
                "a1b2c3" -> "New York region, United States"
                "d4e5f6" -> "Paris region, France"
                else -> "Tokyo region, Japan"
            },
            lat = flight.lat,
            lng = flight.lng,
            observedAt = snapshot.observedAtEpochSeconds?.let { timestamp.format(Instant.ofEpochSecond(it)) } ?: "Unknown",
            severity = if (flight.onGround == true) "On ground" else "Demo aircraft",
            summary = "Fictional aircraft for the native flight radar preview. Position and telemetry are synthetic, not a live flight or a real flight route.",
            flight = flight,
            provider = snapshot.provider,
            coverage = snapshot.coverage,
        )
    }
}
