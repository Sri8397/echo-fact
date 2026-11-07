package com.taurotechnologies.godseye.nativeapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Channel id the web push worker targets too (see src/components/mobile/ and the Capacitor app). */
const val ALERT_CHANNEL_ID: String = "godseye-alerts"

/** Creates the alert channel once. Android 8+ only; the app's minSdk is 26, so the check is a formality. */
fun ensureAlertChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(ALERT_CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(ALERT_CHANNEL_ID, "God's Eye alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Important intelligence alerts"
            // Same as the web channel (visibility 0): content stays private on the lock screen.
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        },
    )
}
