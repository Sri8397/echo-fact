package com.taurotechnologies.godseye.nativeapp.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayBackStackTest {
    @Test
    fun backClosesNewestOverlayFirstLikeTheWebHistory() {
        val stack = OverlayBackStack<NativeOverlay>()
        stack.open(NativeOverlay.ALERT_LIST)
        stack.open(NativeOverlay.ALERT_DETAIL)

        assertEquals(NativeOverlay.ALERT_DETAIL, stack.back())
        assertEquals(listOf(NativeOverlay.ALERT_LIST), stack.stack.value)
        assertEquals(NativeOverlay.ALERT_LIST, stack.back())
        assertNull(stack.back())
    }

    @Test
    fun backWithNoOverlayReturnsNullSoScreenNavigationHandlesIt() {
        val stack = OverlayBackStack<NativeOverlay>()
        assertNull(stack.back())
        assertNull(stack.top())
    }

    @Test
    fun reopeningAnOpenOverlayDoesNotStackItTwiceOrReorderIt() {
        val stack = OverlayBackStack<String>()
        assertTrue(stack.open("a"))
        assertTrue(stack.open("b"))
        assertFalse(stack.open("a"))
        assertEquals(listOf("a", "b"), stack.stack.value)
        assertEquals("b", stack.back())
        assertEquals("a", stack.back())
    }

    @Test
    fun controlCloseRemovesOnlyThatOverlayAndKeepsTheOnesAboveAndBeneathIt() {
        val stack = OverlayBackStack<String>()
        stack.open("list")
        stack.open("detail")
        stack.close("list")

        assertEquals(listOf("detail"), stack.stack.value)
        assertEquals("detail", stack.back())
        assertNull(stack.back())
    }

    @Test
    fun closingAnOverlayThatIsNotOpenIsANoOp() {
        val stack = OverlayBackStack<String>()
        stack.open("list")
        stack.close("missing")
        assertEquals(listOf("list"), stack.stack.value)
    }

    @Test
    fun clearDropsEveryOverlay() {
        val stack = OverlayBackStack<NativeOverlay>()
        stack.open(NativeOverlay.ALERT_LIST)
        stack.open(NativeOverlay.ALERT_DETAIL)
        stack.clear()
        assertTrue(stack.stack.value.isEmpty())
        assertNull(stack.back())
    }
}
