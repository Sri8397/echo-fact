package com.taurotechnologies.godseye.nativeapp.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertInboxRulesTest {
    private val now = 1_800_000_000_000L

    private fun alert(id: String, createdAt: Long = now, title: String = "T$id", read: Long? = null) = NativeAlert(
        id = id, title = title, body = "", category = AlertCategory.NEWS, severity = AlertSeverity.HIGH,
        createdAtMillis = createdAt, readAtMillis = read, panelId = null,
    )

    @Test
    fun addKeepsNewestFirstAndDoesNotDuplicateTheSameContent() {
        var state = AlertInboxState()
        state = AlertInboxRules.add(state, alert("a", now - 10), now)
        state = AlertInboxRules.add(state, alert("b", now), now)
        val again = AlertInboxRules.add(state, alert("b", now), now)
        assertEquals(listOf("b", "a"), state.items.map { it.id })
        assertEquals(state, again)
    }

    @Test
    fun refreshingAnAlertKeepsItsReadState() {
        var state = AlertInboxRules.add(AlertInboxState(), alert("a"), now)
        state = AlertInboxRules.markRead(state, "a", now + 1)
        state = AlertInboxRules.add(state, alert("a", title = "Changed"), now + 2)
        assertEquals("Changed", state.items.single().title)
        assertEquals(now + 1, state.items.single().readAtMillis)
    }

    @Test
    fun dismissedAlertsCannotComeBackFromAPush() {
        var state = AlertInboxRules.add(AlertInboxState(), alert("a"), now)
        state = AlertInboxRules.dismiss(state, "a")
        state = AlertInboxRules.add(state, alert("a"), now)
        assertTrue(state.items.isEmpty())
        assertEquals(listOf("a"), state.dismissedIds)
    }

    @Test
    fun capsTheInboxAtOneHundredItems() {
        var state = AlertInboxState()
        for (index in 0 until 105) state = AlertInboxRules.add(state, alert("i$index", now - index), now)
        assertEquals(AlertInboxRules.MAX_ITEMS, state.items.size)
        assertEquals("i0", state.items.first().id)
    }

    @Test
    fun hidesAndDropsAlertsOutsideTheSevenDayWindow() {
        val old = alert("old", createdAt = now - AlertPayload.RETENTION_MILLIS - 1)
        val state = AlertInboxState(items = listOf(old, alert("new")))
        assertEquals(listOf("new"), AlertInboxRules.visible(state, now).map { it.id })
        assertEquals(1, state.unreadCount(now))
    }

    @Test
    fun markReadAndMarkAllReadOnlyTouchUnreadItems() {
        var state = AlertInboxState(items = listOf(alert("a"), alert("b", read = now - 5)))
        state = AlertInboxRules.markAllRead(state, now)
        assertEquals(now, state.items.first { it.id == "a" }.readAtMillis)
        assertEquals(now - 5, state.items.first { it.id == "b" }.readAtMillis)
        assertEquals(0, state.unreadCount(now))
    }

    @Test
    fun clearReadRemovesReadItemsAndRemembersThemAsDismissed() {
        val state = AlertInboxState(items = listOf(alert("a", read = now), alert("b")))
        val cleared = AlertInboxRules.clearRead(state)
        assertEquals(listOf("b"), cleared.items.map { it.id })
        assertEquals(listOf("a"), cleared.dismissedIds)
    }

    @Test
    fun markReadOnAnUnknownIdIsANoOp() {
        val state = AlertInboxState(items = listOf(alert("a")))
        assertEquals(state, AlertInboxRules.markRead(state, "zzz", now))
        assertNotNull(state.items.single())
        assertFalse(state.items.single().readAtMillis != null)
        assertNull(AlertInboxRules.visible(AlertInboxState(), now).firstOrNull())
    }
}
