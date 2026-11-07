package com.taurotechnologies.godseye.nativeapp.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertPayloadTest {
    private val now = 1_800_000_000_000L

    private fun payload(vararg pairs: Pair<String, String>): Map<String, String> = mapOf(
        "schema" to "godseye-alert-v1",
        "id" to "intel:42",
        "title" to "  Border activity  ",
        "body" to "Summary",
        "category" to "security",
        "severity" to "critical",
        "createdAt" to now.toString(),
        "panelId" to "strategic-risk",
    ) + pairs

    @Test
    fun parsesAValidWebAlertAndTrimsTheTitle() {
        val alert = AlertPayload.parse(payload(), now)!!
        assertEquals("intel:42", alert.id)
        assertEquals("Border activity", alert.title)
        assertEquals(AlertCategory.SECURITY, alert.category)
        assertEquals(AlertSeverity.CRITICAL, alert.severity)
        assertEquals("strategic-risk", alert.panelId)
        assertNull(alert.readAtMillis)
    }

    @Test
    fun rejectsWrongSchemaOrMissingIdentity() {
        assertNull(AlertPayload.parse(payload("schema" to "godseye-alert-v2"), now))
        assertNull(AlertPayload.parse(payload("id" to ""), now))
        assertNull(AlertPayload.parse(payload("title" to "   "), now))
    }

    @Test
    fun rejectsUnknownCategoryOrSeverity() {
        assertNull(AlertPayload.parse(payload("category" to "weather"), now))
        assertNull(AlertPayload.parse(payload("severity" to "low"), now))
    }

    @Test
    fun rejectsTimestampsOutsideTheRetentionAndSkewWindow() {
        assertNull(AlertPayload.parse(payload("createdAt" to "abc"), now))
        assertNull(AlertPayload.parse(payload("createdAt" to (now - AlertPayload.RETENTION_MILLIS - 1).toString()), now))
        assertNull(AlertPayload.parse(payload("createdAt" to (now + AlertPayload.MAX_FUTURE_SKEW_MILLIS + 1).toString()), now))
    }

    @Test
    fun dropsAMalformedPanelIdButKeepsTheAlert() {
        val alert = AlertPayload.parse(payload("panelId" to "../../etc"), now)!!
        assertNull(alert.panelId)
        assertEquals("intel:42", alert.id)
    }

    @Test
    fun truncatesOversizedFields() {
        val alert = AlertPayload.parse(payload("id" to "x".repeat(300), "title" to "t".repeat(400), "body" to "b".repeat(3000)), now)!!
        assertEquals(200, alert.id.length)
        assertEquals(300, alert.title.length)
        assertEquals(2000, alert.body.length)
    }
}
