package com.just_sri.suvidha.radar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

class MockRadarApiTest {
    private val timestampFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd HH:mm 'UTC'")
        .withResolverStyle(ResolverStyle.STRICT)

    @Test
    fun fixturesCoverEveryLayerWithUniqueStableIdsAndValidCoordinates() {
        val firstRead: List<RadarSignal> = MockRadarApi.signals()
        val secondRead: List<RadarSignal> = MockRadarApi.signals()

        assertEquals(RadarLayer.entries.toSet(), firstRead.map { it.layer }.toSet())
        assertEquals(firstRead.map { it.id }.toSet().size, firstRead.size)
        assertEquals(firstRead.map { it.id }, secondRead.map { it.id })
        assertTrue(firstRead.all { it.id.isNotBlank() })
        assertTrue(firstRead.all { it.lat.isFinite() && it.lat in -90.0..90.0 })
        assertTrue(firstRead.all { it.lng.isFinite() && it.lng in -180.0..180.0 })

        for (layer in RadarLayer.entries) {
            assertFalse("$layer should have at least one fixture", firstRead.none { it.layer == layer })
        }
    }

    @Test
    fun fixtureTimesAreStrictUtc() {
        for (signal in MockRadarApi.signals()) {
            val parsedTime: LocalDateTime? = try {
                LocalDateTime.parse(signal.observedAt, timestampFormatter)
            } catch (_: Exception) {
                null
            }
            assertNotNull("Invalid UTC timestamp for ${signal.id}: ${signal.observedAt}", parsedTime)
        }
    }

    @Test
    fun everyLayerHasTwoDistinctWellFormedOfficialSourceReferences() {
        for (layer in RadarLayer.entries) {
            val references: List<RadarSourceReference> = layer.sourceReferences()
            assertEquals("$layer should have two source references", 2, references.size)

            for (reference in references) {
                assertTrue("Publisher missing for $layer", reference.publisher.isNotBlank())
                assertTrue("Title missing for $layer", reference.title.isNotBlank())
                assertTrue("Description missing for $layer", reference.description.isNotBlank())

                val sourceUri: URI? = try {
                    URI(reference.url)
                } catch (_: Exception) {
                    null
                }
                assertNotNull("Invalid source URL for $layer: ${reference.url}", sourceUri)
                assertEquals("https", sourceUri?.scheme)
                assertTrue("Source host missing for $layer", !sourceUri?.host.isNullOrBlank())
            }

            assertEquals(
                "$layer source URLs should be distinct",
                references.map { it.url }.toSet().size,
                references.size,
            )
        }
    }
}
