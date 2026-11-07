package com.taurotechnologies.godseye.nativeapp.notifications

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.taurotechnologies.godseye.nativeapp.nav.DeepLink
import com.taurotechnologies.godseye.nativeapp.nav.NativeOverlay
import com.taurotechnologies.godseye.nativeapp.nav.OverlayBackStack
import com.taurotechnologies.godseye.nativeapp.nav.parseDeepLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the activity should do after an incoming link or notification tap. Overlays are already updated. */
sealed interface NavigationRequest {
    data class OpenPanel(val panelId: String) : NavigationRequest
}

/**
 * Owns the alert overlays, the notification permission state, and routing for taps. Survives rotation.
 * Routing never touches Screen navigation directly: the activity applies an [NavigationRequest.OpenPanel].
 */
class AlertsViewModel(application: Application) : AndroidViewModel(application) {
    val overlays = OverlayBackStack<NativeOverlay>()

    private val mutableSelected = MutableStateFlow<NativeAlert?>(null)
    /** The alert shown on the detail overlay. Set when a notification opens the detail. */
    val selected: StateFlow<NativeAlert?> = mutableSelected.asStateFlow()

    private val mutablePermission = MutableStateFlow(PushPermissionState.NOT_ASKED)
    val permission: StateFlow<PushPermissionState> = mutablePermission.asStateFlow()

    private val preferences = application.getSharedPreferences(PREFERENCES, android.content.Context.MODE_PRIVATE)

    init {
        AlertInboxStore.initialize(application)
    }

    fun openList() {
        overlays.open(NativeOverlay.ALERT_LIST)
    }

    fun openDetail(alert: NativeAlert) {
        mutableSelected.value = alert
        overlays.open(NativeOverlay.ALERT_LIST)
        overlays.open(NativeOverlay.ALERT_DETAIL)
    }

    /** Closes every alert overlay, e.g. before a panel opens above the screen stack. */
    fun closeAll() {
        overlays.clear()
        mutableSelected.value = null
    }

    /** Control close (the header back arrow, or closing after an action). Does not affect overlays beneath. */
    fun closeOverlay(id: NativeOverlay) {
        overlays.close(id)
        if (id == NativeOverlay.ALERT_DETAIL) mutableSelected.value = null
    }

    /** System Back. Returns true when an overlay consumed it; false means the screen stack should handle it. */
    fun back(): Boolean {
        val closed = overlays.back() ?: return false
        if (closed == NativeOverlay.ALERT_DETAIL) mutableSelected.value = null
        return true
    }

    fun refreshPermission(runtimePermissionRequired: Boolean, granted: Boolean, shouldShowRationale: Boolean) {
        mutablePermission.value = pushPermissionState(
            runtimePermissionRequired = runtimePermissionRequired,
            granted = granted,
            askedBefore = preferences.getBoolean(KEY_ASKED, false),
            shouldShowRationale = shouldShowRationale,
        )
    }

    /** Call just before launching the system permission prompt, so a later denial reads as denied, not never-asked. */
    fun markPermissionAsked() {
        preferences.edit().putBoolean(KEY_ASKED, true).apply()
    }

    /**
     * Routes a launch or new intent. Handles godseye:// links and FCM-style data extras (a notification tap).
     * Returns null when the intent carries nothing valid. The intent is cleared so rotation does not re-route it.
     */
    fun routeIntent(intent: Intent?, nowMillis: Long, isPanelKnown: (String) -> Boolean): NavigationRequest? {
        if (intent == null) return null
        val request = routeDeepLink(intent.dataString, isPanelKnown)
            ?: routeNotificationTap(intent, nowMillis, isPanelKnown)
        intent.data = null
        intent.replaceExtras(android.os.Bundle())
        return request
    }

    private fun routeDeepLink(raw: String?, isPanelKnown: (String) -> Boolean): NavigationRequest? {
        return when (val link = parseDeepLink(raw)) {
            null -> null
            DeepLink.Alerts -> { openList(); null }
            is DeepLink.Panel -> if (isPanelKnown(link.panelId)) {
                overlays.clear()
                NavigationRequest.OpenPanel(link.panelId)
            } else null
        }
    }

    private fun routeNotificationTap(intent: Intent, nowMillis: Long, isPanelKnown: (String) -> Boolean): NavigationRequest? {
        val extras = intent.extras ?: return null
        val data: Map<String, String> = extras.keySet().mapNotNull { key ->
            (extras.get(key) as? String)?.let { key to it }
        }.toMap()
        val alert = AlertPayload.parse(data, nowMillis) ?: return null
        AlertInboxStore.add(getApplication(), alert, nowMillis)
        AlertInboxStore.markRead(getApplication(), alert.id, nowMillis)
        val panelId = alert.panelId
        if (panelId != null && isPanelKnown(panelId)) {
            overlays.clear()
            mutableSelected.value = null
            return NavigationRequest.OpenPanel(panelId)
        }
        overlays.clear()
        openDetail(alert)
        return null
    }

    private companion object {
        const val PREFERENCES = "godseye_push_permission"
        const val KEY_ASKED = "notification_permission_asked_v1"
    }
}
