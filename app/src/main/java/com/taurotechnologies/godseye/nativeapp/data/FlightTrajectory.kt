package com.taurotechnologies.godseye.nativeapp.data

import java.util.concurrent.ConcurrentHashMap

/** One point on a flight path. */
data class TrackPoint(val lat: Double, val lng: Double, val altitudeMeters: Double?)

/**
 * A selected flight's path: [past] is the positions this app actually observed (oldest first, ending at the
 * current fix); [ahead] is a short dead-reckoned projection along the reported heading and speed. The feed has no route or destination, so
 * only a few minutes are drawn: past that a straight line from the current heading is a guess, not a flight path.
 */
data class FlightTrajectory(val id: String, val past: List<TrackPoint>, val ahead: List<TrackPoint>) {
    val isEmpty: Boolean get() = past.size < 2 && ahead.size < 2
}

/**
 * The feed is a single snapshot with no history, so the observed trail is built from the snapshots this
 * app has loaded in the current run (memory only; nothing is persisted or fetched elsewhere).
 */
object FlightTrackHistory {
    private const val MAX_POINTS = 120
    private const val MAX_AGE_SECONDS = 3600L
    private const val MIN_STEP_DEGREES = 0.0005
    private const val AHEAD_MINUTES = 5
    private const val AHEAD_STEP_SECONDS = 30

    private data class Fix(val point: TrackPoint, val epochSeconds: Long)

    private val tracks = ConcurrentHashMap<String, ArrayDeque<Fix>>()

    /** Records the latest snapshot and forgets aircraft that left it. */
    fun record(flights: List<FlightObservation>, nowEpochSeconds: Long = System.currentTimeMillis() / 1000L) {
        val present = HashSet<String>(flights.size)
        for (flight in flights) {
            present += flight.id
            val time = flight.observedAtEpochSeconds ?: nowEpochSeconds
            val deque = tracks.getOrPut(flight.id) { ArrayDeque() }
            synchronized(deque) {
                val last = deque.lastOrNull()
                val moved = last == null ||
                    kotlin.math.abs(last.point.lat - flight.lat) > MIN_STEP_DEGREES ||
                    kotlin.math.abs(last.point.lng - flight.lng) > MIN_STEP_DEGREES
                if (moved && (last == null || time > last.epochSeconds)) {
                    deque.addLast(Fix(TrackPoint(flight.lat, flight.lng, flight.altitudeMeters), time))
                }
                while (deque.size > MAX_POINTS || (deque.isNotEmpty() && nowEpochSeconds - deque.first().epochSeconds > MAX_AGE_SECONDS)) {
                    deque.removeFirst()
                }
            }
        }
        tracks.keys.retainAll(present)
    }

    fun clear() = tracks.clear()

    fun trajectory(flight: FlightObservation): FlightTrajectory {
        val deque = tracks[flight.id]
        val past = if (deque == null) emptyList() else synchronized(deque) { deque.map { it.point } }
        val current = TrackPoint(flight.lat, flight.lng, flight.altitudeMeters)
        val observed = if (past.lastOrNull()?.let { it.lat == current.lat && it.lng == current.lng } == true) past else past + current
        return FlightTrajectory(flight.id, if (observed.size >= 2) observed else emptyList(), projectAhead(flight))
    }

    internal fun projectAhead(flight: FlightObservation): List<TrackPoint> {
        val speed = flight.speedMetersPerSecond?.takeIf { it.isFinite() } ?: return emptyList()
        val heading = flight.headingDegrees?.takeIf { it.isFinite() } ?: return emptyList()
        if (flight.onGround == true || speed <= FlightMotion.HOLD_SPEED_MPS) return emptyList()
        val steps = AHEAD_MINUTES * 60 / AHEAD_STEP_SECONDS
        return (0..steps).map { i ->
            val (lat, lng) = FlightMotion.destination(flight.lat, flight.lng, heading, speed * AHEAD_STEP_SECONDS * i)
            TrackPoint(lat, lng, flight.altitudeMeters)
        }
    }
}
