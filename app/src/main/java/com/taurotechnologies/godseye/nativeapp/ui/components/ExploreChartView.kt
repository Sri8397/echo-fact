package com.taurotechnologies.godseye.nativeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.data.explore.ExploreChart
import com.taurotechnologies.godseye.nativeapp.data.explore.ExploreChartKind
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import kotlin.math.abs

/**
 * Chart hues, fixed across colour styles and validated with the dataviz palette checks against
 * every style's light and dark surface (CVD ΔE ≥ 19, contrast ≥ 3:1). Text never uses these;
 * values and labels use the theme's ink and muted tokens.
 */
private object ChartHues {
    val seriesLight = Color(0xFF2A78D6)
    val seriesDark = Color(0xFF3987E5)
    val negativeLight = Color(0xFFE34948)
    val negativeDark = Color(0xFFE66767)
}

private val BAR_THICKNESS = 12.dp
private val BAR_END_RADIUS = 4.dp
private val LINE_HEIGHT = 132.dp

/**
 * Shared Explore chart. A single series, so the title names it and there is no legend; the
 * panel's rows below remain the full table view. Bars put the value at the tip; the line labels
 * its latest value and its range.
 */
@Composable
fun ExploreChartView(chart: ExploreChart, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    val series = if (colors.isDark) ChartHues.seriesDark else ChartHues.seriesLight
    val negative = if (colors.isDark) ChartHues.negativeDark else ChartHues.negativeLight
    val description = chart.title + ": " + chart.points.joinToString("; ") { "${it.label} ${it.display}" }
    Surface(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderLight),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(chart.title, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            when (chart.kind) {
                ExploreChartKind.BARS -> Bars(chart, series, series, diverging = false)
                ExploreChartKind.DIVERGING_BARS -> Bars(chart, series, negative, diverging = true)
                ExploreChartKind.LINE -> Line(chart, series)
            }
        }
    }
}

@Composable
private fun Bars(chart: ExploreChart, positive: Color, negative: Color, diverging: Boolean) {
    val colors = LocalSuvidhaColors.current
    val max = chart.points.maxOf { abs(it.value) }.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chart.points.forEach { point ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    point.label, color = colors.ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.36f),
                )
                Spacer(Modifier.width(8.dp))
                BoxWithConstraints(Modifier.weight(0.44f).height(BAR_THICKNESS)) {
                    val fraction = (abs(point.value) / max).toFloat().coerceIn(0f, 1f)
                    if (diverging) {
                        // Centre baseline: positive grows right, negative grows left; square at the baseline.
                        val half = maxWidth / 2
                        Box(Modifier.align(Alignment.Center).width(1.dp).height(BAR_THICKNESS).background(colors.borderLight))
                        val barWidth = half * fraction
                        if (point.value >= 0) {
                            Box(
                                Modifier.align(Alignment.CenterStart).padding(start = half).width(barWidth).height(BAR_THICKNESS)
                                    .background(positive, RoundedCornerShape(topEnd = BAR_END_RADIUS, bottomEnd = BAR_END_RADIUS)),
                            )
                        } else {
                            Box(
                                Modifier.align(Alignment.CenterEnd).padding(end = half).width(barWidth).height(BAR_THICKNESS)
                                    .background(negative, RoundedCornerShape(topStart = BAR_END_RADIUS, bottomStart = BAR_END_RADIUS)),
                            )
                        }
                    } else {
                        Box(
                            Modifier.align(Alignment.CenterStart).width(maxWidth * fraction).height(BAR_THICKNESS)
                                .background(positive, RoundedCornerShape(topEnd = BAR_END_RADIUS, bottomEnd = BAR_END_RADIUS)),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    point.display, color = colors.ink, fontSize = 12.sp, maxLines = 1,
                    modifier = Modifier.weight(0.2f), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun Line(chart: ExploreChart, series: Color) {
    val colors = LocalSuvidhaColors.current
    val points = chart.points
    val min = points.minOf { it.value }
    val max = points.maxOf { it.value }
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val maxPoint = points.maxBy { it.value }
    val minPoint = points.minBy { it.value }
    val latest = points.last()
    val surface = colors.cardSurface
    val grid = colors.borderLight
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("Latest ", color = colors.muted, fontSize = 12.sp)
            Text(latest.display, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("High ${maxPoint.display}  ·  Low ${minPoint.display}", color = colors.muted, fontSize = 11.sp, maxLines = 1)
        }
        Canvas(Modifier.fillMaxWidth().height(LINE_HEIGHT)) {
            val inset = 6.dp.toPx() // room for the end dot and its ring
            val w = size.width - inset * 2
            val h = size.height - inset * 2
            fun x(i: Int) = inset + if (points.size == 1) w / 2 else w * i / (points.size - 1)
            fun y(v: Double) = inset + (h * (1 - (v - min) / span)).toFloat()
            // Recessive hairline gridlines at the high and low.
            drawLine(grid, Offset(0f, y(max)), Offset(size.width, y(max)), strokeWidth = 1.dp.toPx())
            drawLine(grid, Offset(0f, y(min)), Offset(size.width, y(min)), strokeWidth = 1.dp.toPx())
            val line = Path()
            points.forEachIndexed { i, p -> if (i == 0) line.moveTo(x(i), y(p.value)) else line.lineTo(x(i), y(p.value)) }
            val area = Path().apply {
                addPath(line)
                lineTo(x(points.lastIndex), y(min) + 0f)
                lineTo(x(0), y(min))
                close()
            }
            drawPath(area, series.copy(alpha = 0.10f))
            drawPath(line, series, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val end = Offset(x(points.lastIndex), y(latest.value))
            drawCircle(surface, radius = 6.dp.toPx(), center = end)
            drawCircle(series, radius = 4.dp.toPx(), center = end)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(points.first().label, color = colors.muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(latest.label, color = colors.muted, fontSize = 11.sp)
        }
    }
}
