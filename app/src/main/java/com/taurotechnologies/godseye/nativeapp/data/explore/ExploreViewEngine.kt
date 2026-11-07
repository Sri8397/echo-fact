package com.taurotechnologies.godseye.nativeapp.data.explore

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.util.Locale

/**
 * Server-driven panel view, sent in the Explore catalog. The server decides what a panel shows;
 * [ExploreViewEngine] turns the panel's bootstrap data into [PanelContent] for the shared renderer.
 *
 * Paths are dot-separated (`summary.total`, `items.0.name`). Templates replace `{path}` with the
 * value at that path. Unknown block types and unknown formats are ignored, so older apps keep
 * working when the server adds new ones.
 */
@Serializable
data class ExploreView(val blocks: List<ExploreBlock> = emptyList())

@Serializable
data class ExploreBlock(
    val type: String,
    /** summary: the text template, evaluated against the panel root. */
    val text: String? = null,
    /** metrics: fixed label/value pairs read from the panel root. */
    val metrics: List<ExploreMetric> = emptyList(),
    /** list: path to the array of items; null means the panel root is the array. */
    val items: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val value: ExploreValue? = null,
    /** Path to a signed number; colours the row green or red. */
    val change: String? = null,
    /** Path to an http(s) link opened when the row is tapped. */
    val url: String? = null,
    val sort: ExploreSort? = null,
    val limit: Int = DEFAULT_LIMIT,
    /** chart: `bars` (ranked magnitude), `diverging` (above/below zero) or `line` (over time). */
    val kind: String? = null,
)

@Serializable
data class ExploreMetric(val label: String, val value: ExploreValue, val change: String? = null)

@Serializable
data class ExploreValue(
    val path: String,
    /** text, number, price, percent, signed-percent, enum. */
    val format: String = "text",
    val prefix: String = "",
    val suffix: String = "",
    /** enum only: leading text removed before humanizing, e.g. `ALERT_LEVEL_`. */
    val strip: String = "",
)

@Serializable
data class ExploreSort(val path: String, @SerialName("order") val order: String = "desc")

internal const val DEFAULT_LIMIT = 25
private const val MAX_LIMIT = 100
private val TEMPLATE_TOKEN = Regex("\\{([A-Za-z0-9_.]+)\\}")

object ExploreViewEngine {
    /** Evaluates every block in order. Summary blocks join into the panel summary; the rest add rows. */
    fun render(view: ExploreView, root: JsonElement): PanelContent {
        val summaries = mutableListOf<String>()
        val rows = mutableListOf<ExploreRow>()
        var chart: ExploreChart? = null
        for (block in view.blocks) {
            when (block.type) {
                "summary" -> block.text?.let { template(it, root) }?.let(summaries::add)
                "metrics" -> rows += metrics(block, root)
                "list" -> rows += list(block, root)
                "chart" -> if (chart == null) chart = chart(block, root)
                else -> Unit // Unknown block from a newer server: skip it.
            }
        }
        return PanelContent(summary = summaries.joinToString("  ·  ").ifBlank { null }, rows = rows, chart = chart)
    }

    /**
     * Chart block: `text` is the chart title, `items` the array, `title` the point label template and
     * `value` the plotted number with its display format. Uses the shared chart builders, so form rules
     * (minimum points, ranking, limits) match built-in panels.
     */
    private fun chart(block: ExploreBlock, root: JsonElement): ExploreChart? {
        val title = block.text?.let { template(it, root) } ?: return null
        val labelTemplate = block.title ?: return null
        val value = block.value ?: return null
        val source = (if (block.items == null) root else at(root, block.items)) as? JsonArray ?: return null
        val points = source.filterIsInstance<JsonObject>().map { item ->
            (template(labelTemplate, item) ?: "") to number(at(item, value.path))
        }
        val format: (Double) -> String? = { v -> formatValue(value, JsonPrimitive(v)) }
        return when (block.kind) {
            "bars" -> rankedBarChart(title, points, format)
            "diverging" -> divergingBarChart(title, points, format)
            "line" -> lineChart(title, points, format)
            else -> null // Unknown chart kind from a newer catalog.
        }
    }

    private fun metrics(block: ExploreBlock, root: JsonElement): List<ExploreRow> =
        block.metrics.mapNotNull { metric ->
            val value = formatValue(metric.value, root) ?: return@mapNotNull null
            ExploreRow(title = metric.label, value = value, change = metric.change?.let { number(at(root, it)) })
        }

    private fun list(block: ExploreBlock, root: JsonElement): List<ExploreRow> {
        val titleTemplate = block.title ?: return emptyList()
        val source = (if (block.items == null) root else at(root, block.items)) as? JsonArray ?: return emptyList()
        var items: List<JsonElement> = source.filterIsInstance<JsonObject>()
        block.sort?.let { sort ->
            val keyed = items.sortedBy { number(at(it, sort.path)) ?: Double.NEGATIVE_INFINITY }
            items = if (sort.order == "asc") keyed else keyed.reversed()
        }
        return items.asSequence().mapNotNull { item ->
            val title = template(titleTemplate, item) ?: return@mapNotNull null
            ExploreRow(
                title = title,
                subtitle = block.subtitle?.let { template(it, item) },
                value = block.value?.let { formatValue(it, item) },
                change = block.change?.let { number(at(item, it)) },
                url = block.url?.let { text(at(item, it)) }?.takeIf(::isHttpUrl),
            )
        }.take(block.limit.coerceIn(1, MAX_LIMIT)).toList()
    }

    /** Replaces each `{path}`. Returns null when every placeholder is missing or the result is blank. */
    internal fun template(template: String, scope: JsonElement): String? {
        var found = TEMPLATE_TOKEN.findAll(template).none()
        val result = TEMPLATE_TOKEN.replace(template) { match ->
            text(at(scope, match.groupValues[1]))?.also { found = true }.orEmpty()
        }.trim().replace(Regex("\\s{2,}"), " ").trim(' ', '·', '-', ',', '|')
        return result.takeIf { found && it.isNotBlank() }
    }

    internal fun formatValue(value: ExploreValue, scope: JsonElement): String? {
        // A bare number is formatted directly (chart values); otherwise read the path.
        val element = if (scope is JsonPrimitive) scope else at(scope, value.path)
        val formatted = when (value.format) {
            "number" -> formatNumber(number(element))
            "price" -> formatPrice(number(element))
            "signed-percent" -> formatPercent(number(element))
            "percent" -> number(element)?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.1f%%", it) }
            // A 0-1 probability (e.g. a prediction market's Yes price) shown as a percentage.
            "probability" -> number(element)?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.1f%%", it * 100) }
            "enum" -> humanEnum(text(element), value.strip)
            else -> text(element)
        } ?: return null
        return value.prefix + formatted + value.suffix
    }

    /** Value at a dot path; numeric segments index arrays. */
    internal fun at(scope: JsonElement?, path: String): JsonElement? =
        path.split('.').filter { it.isNotEmpty() }.fold(scope) { current, segment ->
            when (current) {
                is JsonObject -> current[segment]
                is JsonArray -> segment.toIntOrNull()?.let { current.getOrNull(it) }
                else -> null
            }
        }

    private fun number(element: JsonElement?): Double? =
        (element as? JsonPrimitive)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }?.takeIf { it.isFinite() }

    private fun text(element: JsonElement?): String? {
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive.isString) return primitive.content.takeIf { it.isNotBlank() }
        val number = primitive.doubleOrNull ?: return primitive.content.takeIf { it != "null" }
        return if (number == Math.floor(number) && kotlin.math.abs(number) < 1e15) number.toLong().toString()
        else String.format(Locale.US, "%.2f", number)
    }

    private fun isHttpUrl(url: String): Boolean = url.startsWith("https://") || url.startsWith("http://")
}
