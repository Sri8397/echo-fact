package com.taurotechnologies.godseye.nativeapp.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class PushPermissionStateTest {
    @Test
    fun preAndroid13NeedsNoRuntimePermission() {
        assertEquals(PushPermissionState.NOT_REQUIRED, pushPermissionState(false, granted = false, askedBefore = false, shouldShowRationale = false))
    }

    @Test
    fun grantedWinsOverEveryOtherSignal() {
        assertEquals(PushPermissionState.GRANTED, pushPermissionState(true, granted = true, askedBefore = true, shouldShowRationale = false))
    }

    @Test
    fun neverAskedIsNotMistakenForPermanentDenial() {
        // Android reports "no rationale" before the first request too, so the asked flag decides.
        assertEquals(PushPermissionState.NOT_ASKED, pushPermissionState(true, granted = false, askedBefore = false, shouldShowRationale = false))
    }

    @Test
    fun firstDenialCanBeRetried() {
        assertEquals(PushPermissionState.DENIED_RETRYABLE, pushPermissionState(true, granted = false, askedBefore = true, shouldShowRationale = true))
    }

    @Test
    fun deniedWithoutRationaleAfterAskingMeansSettingsOnly() {
        assertEquals(PushPermissionState.DENIED_PERMANENT, pushPermissionState(true, granted = false, askedBefore = true, shouldShowRationale = false))
    }
}
