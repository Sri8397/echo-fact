package com.taurotechnologies.godseye.nativeapp.notifications

import android.content.Context
import android.util.Log
import com.taurotechnologies.godseye.nativeapp.nav.isValidPanelId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Device-local alert inbox, persisted in SharedPreferences. A corrupt store starts empty instead of crashing. */
object AlertInboxStore {
    private const val PREFERENCES = "godseye_alerts"
    private const val KEY_STATE = "inbox_v1"
    private const val TAG = "AlertInbox"

    private val mutableState = MutableStateFlow(AlertInboxState())
    val state: StateFlow<AlertInboxState> = mutableState.asStateFlow()

    private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val saved = try {
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY_STATE, null)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Alert inbox could not be read", error)
            null
        }
        mutableState.value = saved?.let(::decode) ?: AlertInboxState()
    }

    @Synchronized
    fun add(context: Context, alert: NativeAlert, nowMillis: Long): Boolean = update(context) { AlertInboxRules.add(it, alert, nowMillis) }

    @Synchronized
    fun markRead(context: Context, id: String, nowMillis: Long) = update(context) { AlertInboxRules.markRead(it, id, nowMillis) }

    @Synchronized
    fun markAllRead(context: Context, nowMillis: Long) = update(context) { AlertInboxRules.markAllRead(it, nowMillis) }

    @Synchronized
    fun dismiss(context: Context, id: String) = update(context) { AlertInboxRules.dismiss(it, id) }

    @Synchronized
    fun clearRead(context: Context) = update(context) { AlertInboxRules.clearRead(it) }

    private fun update(context: Context, transform: (AlertInboxState) -> AlertInboxState): Boolean {
        val before = mutableState.value
        val after = transform(before)
        if (after == before) return false
        mutableState.value = after
        try {
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putString(KEY_STATE, encode(after)).apply()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Alert inbox could not be saved; it stays usable in memory", error)
        }
        return true
    }

    internal fun encode(state: AlertInboxState): String = JSONObject()
        .put("items", JSONArray().apply {
            state.items.forEach { alert ->
                put(JSONObject()
                    .put("id", alert.id)
                    .put("title", alert.title)
                    .put("body", alert.body)
                    .put("category", alert.category.wire)
                    .put("severity", alert.severity.wire)
                    .put("createdAt", alert.createdAtMillis)
                    .put("readAt", alert.readAtMillis ?: JSONObject.NULL)
                    .put("panelId", alert.panelId ?: JSONObject.NULL))
            }
        })
        .put("dismissed", JSONArray(state.dismissedIds))
        .toString()

    internal fun decode(text: String): AlertInboxState = try {
        val root = JSONObject(text)
        val items = root.optJSONArray("items") ?: JSONArray()
        val alerts = (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val category = AlertCategory.from(item.optString("category")) ?: return@mapNotNull null
            val severity = AlertSeverity.from(item.optString("severity")) ?: return@mapNotNull null
            val id = item.optString("id").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val panelId = item.optString("panelId").takeIf { it.isNotEmpty() && isValidPanelId(it) }
            NativeAlert(
                id = id,
                title = item.optString("title"),
                body = item.optString("body"),
                category = category,
                severity = severity,
                createdAtMillis = item.optLong("createdAt"),
                readAtMillis = if (item.isNull("readAt")) null else item.optLong("readAt"),
                panelId = panelId,
            )
        }
        val dismissed = root.optJSONArray("dismissed") ?: JSONArray()
        AlertInboxState(
            items = alerts.take(AlertInboxRules.MAX_ITEMS),
            dismissedIds = (0 until dismissed.length()).mapNotNull { dismissed.optString(it).takeIf { id -> id.isNotEmpty() } }
                .takeLast(AlertInboxRules.MAX_DISMISSED),
        )
    } catch (error: org.json.JSONException) {
        Log.w(TAG, "Alert inbox data was unreadable and was reset", error)
        AlertInboxState()
    }
}
