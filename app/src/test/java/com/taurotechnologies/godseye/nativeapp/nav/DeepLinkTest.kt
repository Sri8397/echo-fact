package com.taurotechnologies.godseye.nativeapp.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkTest {
    @Test
    fun acceptsAlertsAndPanelLinks() {
        assertEquals(DeepLink.Alerts, parseDeepLink("godseye://alerts"))
        assertEquals(DeepLink.Alerts, parseDeepLink("godseye://alerts/"))
        assertEquals(DeepLink.Panel("strategic-risk"), parseDeepLink("godseye://panel/strategic-risk"))
        assertEquals(DeepLink.Panel("a"), parseDeepLink("godseye://panel/a"))
        assertEquals(DeepLink.Panel("oref-sirens"), parseDeepLink("GODSEYE://panel/oref-sirens"))
    }

    @Test
    fun rejectsUnknownSchemes() {
        assertNull(parseDeepLink("https://panel/strategic-risk"))
        assertNull(parseDeepLink("http://alerts"))
        assertNull(parseDeepLink("com.taurotechnologies.godseye://alerts"))
        assertNull(parseDeepLink("javascript://panel/strategic-risk"))
        assertNull(parseDeepLink("godseye:panel/strategic-risk"))
    }

    @Test
    fun rejectsUnknownHosts() {
        assertNull(parseDeepLink("godseye://settings"))
        assertNull(parseDeepLink("godseye://evil.example/panel/strategic-risk"))
        assertNull(parseDeepLink("godseye://panel.evil/strategic-risk"))
        assertNull(parseDeepLink("godseye:///panel/strategic-risk"))
        assertNull(parseDeepLink("godseye://www.worldmonitor.app/alerts"))
    }

    @Test
    fun rejectsMalformedPanelIds() {
        assertNull(parseDeepLink("godseye://panel/"))
        assertNull(parseDeepLink("godseye://panel"))
        assertNull(parseDeepLink("godseye://panel/Strategic-Risk"))
        assertNull(parseDeepLink("godseye://panel/../alerts"))
        assertNull(parseDeepLink("godseye://panel/a/b"))
        assertNull(parseDeepLink("godseye://panel/a%2Fb"))
        assertNull(parseDeepLink("godseye://panel/a_b"))
        assertNull(parseDeepLink("godseye://panel/a.b"))
        assertNull(parseDeepLink("godseye://panel/" + "a".repeat(81)))
    }

    @Test
    fun rejectsExtraAuthorityQueryFragmentAndPathOnAlerts() {
        assertNull(parseDeepLink("godseye://alerts/extra"))
        assertNull(parseDeepLink("godseye://user@alerts"))
        assertNull(parseDeepLink("godseye://alerts:8080"))
        assertNull(parseDeepLink("godseye://alerts?x=1"))
        assertNull(parseDeepLink("godseye://alerts#top"))
        assertNull(parseDeepLink("godseye://panel/strategic-risk?next=https://evil.example"))
    }

    @Test
    fun rejectsEmptyWhitespaceControlAndOversizedInput() {
        assertNull(parseDeepLink(null))
        assertNull(parseDeepLink(""))
        assertNull(parseDeepLink(" godseye://alerts"))
        assertNull(parseDeepLink("godseye://alerts\n"))
        assertNull(parseDeepLink("godseye://panel/" + "a".repeat(600)))
        assertNull(parseDeepLink("godseye://panel/a\u0000"))
    }

    @Test
    fun panelIdRuleMatchesTheWebNotificationRule() {
        assertTrue(isValidPanelId("radiation-watch"))
        assertFalse(isValidPanelId(null))
        assertFalse(isValidPanelId(""))
        assertFalse(isValidPanelId("UPPER"))
    }
}
