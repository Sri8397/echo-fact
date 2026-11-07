package com.taurotechnologies.godseye.nativeapp.nav

import java.net.URI
import java.net.URISyntaxException

/** Links the native app accepts. Anything else is rejected, never routed. */
sealed interface DeepLink {
    /** godseye://alerts opens the in-app alert list. */
    data object Alerts : DeepLink

    /** godseye://panel/{id} opens an Explore panel. [panelId] is already validated. */
    data class Panel(val panelId: String) : DeepLink
}

const val DEEP_LINK_SCHEME: String = "godseye"
private const val MAX_LINK_LENGTH: Int = 512
private val PANEL_ID_PATTERN: Regex = Regex("^[a-z0-9-]{1,80}$")

/** Same rule the web app applies to a notification's panelId. */
fun isValidPanelId(id: String?): Boolean = id != null && PANEL_ID_PATTERN.matches(id)

/**
 * Parses a deep link. Returns null for an unknown scheme, host, or path, for any user info, port,
 * query, or fragment, and for an id that is not a plain lowercase slug (so no encoded slashes or dots).
 */
fun parseDeepLink(raw: String?): DeepLink? {
    if (raw.isNullOrEmpty() || raw.length > MAX_LINK_LENGTH) return null
    if (raw.any { it.isWhitespace() || it.code < 0x20 }) return null
    val uri: URI = try {
        URI(raw)
    } catch (_: URISyntaxException) {
        return null
    }
    if (!uri.isAbsolute || !uri.scheme.equals(DEEP_LINK_SCHEME, ignoreCase = true)) return null
    if (uri.rawUserInfo != null || uri.port != -1 || uri.rawQuery != null || uri.rawFragment != null) return null
    val host: String = uri.host?.lowercase() ?: return null
    val path: String = uri.rawPath.orEmpty()
    return when (host) {
        "alerts" -> if (path.isEmpty() || path == "/") DeepLink.Alerts else null
        "panel" -> {
            if (!path.startsWith("/")) return null
            val id = path.substring(1)
            if (isValidPanelId(id)) DeepLink.Panel(id) else null
        }
        else -> null
    }
}
