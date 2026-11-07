package com.taurotechnologies.godseye.nativeapp.notifications

/**
 * Where notification permission stands for this app. Android returns "no rationale" both before the
 * first request and after the user picked "don't ask again", so [askedBefore] tells those apart.
 */
enum class PushPermissionState { NOT_REQUIRED, GRANTED, NOT_ASKED, DENIED_RETRYABLE, DENIED_PERMANENT }

/**
 * @param runtimePermissionRequired true on Android 13 (API 33) and newer, where POST_NOTIFICATIONS is a runtime permission.
 * @param shouldShowRationale ActivityCompat.shouldShowRequestPermissionRationale for POST_NOTIFICATIONS.
 */
fun pushPermissionState(
    runtimePermissionRequired: Boolean,
    granted: Boolean,
    askedBefore: Boolean,
    shouldShowRationale: Boolean,
): PushPermissionState = when {
    !runtimePermissionRequired -> PushPermissionState.NOT_REQUIRED
    granted -> PushPermissionState.GRANTED
    !askedBefore -> PushPermissionState.NOT_ASKED
    shouldShowRationale -> PushPermissionState.DENIED_RETRYABLE
    else -> PushPermissionState.DENIED_PERMANENT
}
