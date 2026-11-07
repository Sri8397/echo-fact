package com.taurotechnologies.godseye.nativeapp.notifications

import com.taurotechnologies.godseye.nativeapp.nav.isValidPanelId

/** Same categories and severities the web inbox uses (src/types/notifications.ts). */
enum class AlertCategory(val wire: String) { NEWS("news"), SECURITY("security"), MARKETS("markets"), DISASTERS("disasters"), SYSTEM("system");

    companion object {
        fun from(value: String?): AlertCategory? = entries.firstOrNull { it.wire == value }
    }
}

enum class AlertSeverity(val wire: String, val rank: Int) { CRITICAL("critical", 0), HIGH("high", 1), MEDIUM("medium", 2);

    companion object {
        fun from(value: String?): AlertSeverity? = entries.firstOrNull { it.wire == value }
    }
}

/** One inbox entry. Stored on this device only; no push token or credential is kept with it. */
data class NativeAlert(
    val id: String,
    val title: String,
    val body: String,
    val category: AlertCategory,
    val severity: AlertSeverity,
    val createdAtMillis: Long,
    val readAtMillis: Long?,
    val panelId: String?,
)

/** Parses the data payload the web push worker sends (schema godseye-alert-v1). Same rules as normalizeNotification. */
object AlertPayload {
    const val SCHEMA: String = "godseye-alert-v1"
    const val RETENTION_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    const val MAX_FUTURE_SKEW_MILLIS: Long = 300_000L

    /** [data] holds string values as delivered by FCM or an Android intent. Returns null for anything unexpected. */
    fun parse(data: Map<String, String>, nowMillis: Long): NativeAlert? {
        if (data["schema"] != SCHEMA) return null
        val id = data["id"]?.takeIf { it.isNotEmpty() } ?: return null
        val title = data["title"]?.trim().orEmpty()
        if (title.isEmpty()) return null
        val category = AlertCategory.from(data["category"]) ?: return null
        val severity = AlertSeverity.from(data["severity"]) ?: return null
        val createdAt = data["createdAt"]?.toLongOrNull() ?: return null
        if (createdAt < nowMillis - RETENTION_MILLIS || createdAt > nowMillis + MAX_FUTURE_SKEW_MILLIS) return null
        val panelId = data["panelId"]?.takeIf { isValidPanelId(it) }
        return NativeAlert(
            id = id.take(200),
            title = title.take(300),
            body = data["body"].orEmpty().take(2000),
            category = category,
            severity = severity,
            createdAtMillis = createdAt,
            readAtMillis = null,
            panelId = panelId,
        )
    }
}
