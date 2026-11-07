package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.Locale

/** Small, null-safe JSON helpers shared by the Explore parsers. */
internal fun JsonElement?.obj(): JsonObject? = this as? JsonObject
internal fun JsonElement?.arr(): JsonArray? = this as? JsonArray
internal fun JsonObject?.str(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
internal fun JsonObject?.num(key: String): Double? = (this?.get(key) as? JsonPrimitive)?.doubleOrNull
internal fun JsonObject?.long(key: String): Long? = (this?.get(key) as? JsonPrimitive)?.longOrNull
internal fun JsonObject?.bool(key: String): Boolean? = (this?.get(key) as? JsonPrimitive)?.booleanOrNull
internal fun JsonObject?.list(key: String): List<JsonObject> = (this?.get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

/** Prices: two decimals under 1,000, none above; very small prices keep four decimals. */
internal fun formatPrice(value: Double?): String? = when {
    value == null || !value.isFinite() -> null
    kotlin.math.abs(value) >= 1000 -> String.format(Locale.US, "%,.0f", value)
    kotlin.math.abs(value) >= 1 -> String.format(Locale.US, "%,.2f", value)
    else -> String.format(Locale.US, "%.4f", value)
}

internal fun formatPercent(value: Double?): String? =
    if (value == null || !value.isFinite()) null else String.format(Locale.US, "%+.2f%%", value)

internal fun formatNumber(value: Double?): String? = when {
    value == null || !value.isFinite() -> null
    kotlin.math.abs(value) >= 1e12 -> String.format(Locale.US, "%.2fT", value / 1e12)
    kotlin.math.abs(value) >= 1e9 -> String.format(Locale.US, "%.2fB", value / 1e9)
    kotlin.math.abs(value) >= 1e6 -> String.format(Locale.US, "%.2fM", value / 1e6)
    kotlin.math.abs(value) >= 1e3 -> String.format(Locale.US, "%,.0f", value)
    else -> String.format(Locale.US, "%.2f", value)
}

/** Turns API enum strings like "SERVICE_OPERATIONAL_STATUS_OPERATIONAL" into "Operational". */
internal fun humanEnum(value: String?, prefix: String = ""): String? = value
    ?.removePrefix(prefix)
    ?.replace('_', ' ')
    ?.lowercase(Locale.ROOT)
    ?.replaceFirstChar { it.titlecase(Locale.ROOT) }
