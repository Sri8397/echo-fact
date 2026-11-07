package com.taurotechnologies.godseye.nativeapp.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import com.taurotechnologies.godseye.nativeapp.data.RadarLayer
import java.io.ByteArrayOutputStream

// Per-event marker icons (planes, satellites and one per layer), restored from the old native map
// (android-native/backup/map). Rendered to PNG data URLs for the Cesium page, which has no bitmap API.

private const val PLANE_NORMAL = "plane-normal"
private const val PLANE_SELECTED = "plane-selected"
private const val SAT_NORMAL = "sat-normal"
private const val SAT_SELECTED = "sat-selected"
private const val SELECTED_ARGB = 0xFFFFD166.toInt()

internal fun createAirplaneBitmap(
    fillColor: Int,
    strokeColor: Int = android.graphics.Color.argb(220, 10, 15, 20),
    sizePx: Int = 72,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f

    val path = Path().apply {
        moveTo(16f * scale, 2f * scale)
        lineTo(17.5f * scale, 10f * scale)
        lineTo(17f * scale, 12f * scale)
        lineTo(27f * scale, 17f * scale)
        lineTo(27f * scale, 19f * scale)
        lineTo(17f * scale, 16f * scale)
        lineTo(17f * scale, 24f * scale)
        lineTo(20f * scale, 26.5f * scale)
        lineTo(20f * scale, 28f * scale)
        lineTo(16f * scale, 27f * scale)
        lineTo(12f * scale, 28f * scale)
        lineTo(12f * scale, 26.5f * scale)
        lineTo(15f * scale, 24f * scale)
        lineTo(15f * scale, 16f * scale)
        lineTo(5f * scale, 19f * scale)
        lineTo(5f * scale, 17f * scale)
        lineTo(15f * scale, 12f * scale)
        lineTo(14.5f * scale, 10f * scale)
        close()
    }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * scale
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)
    return bitmap
}

internal fun createSatelliteBitmap(
    fillColor: Int,
    strokeColor: Int = android.graphics.Color.argb(220, 10, 15, 20),
    sizePx: Int = 72,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f

    val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 56, 214, 255)
        style = Paint.Style.FILL
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * scale
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val boomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * scale
    }

    canvas.drawRect(3f * scale, 12f * scale, 10f * scale, 20f * scale, panelPaint)
    canvas.drawRect(3f * scale, 12f * scale, 10f * scale, 20f * scale, strokePaint)
    canvas.drawRect(22f * scale, 12f * scale, 29f * scale, 20f * scale, panelPaint)
    canvas.drawRect(22f * scale, 12f * scale, 29f * scale, 20f * scale, strokePaint)

    canvas.drawLine(10f * scale, 16f * scale, 22f * scale, 16f * scale, boomPaint)

    val body = Path().apply {
        moveTo(16f * scale, 10f * scale)
        lineTo(21f * scale, 16f * scale)
        lineTo(16f * scale, 22f * scale)
        lineTo(11f * scale, 16f * scale)
        close()
    }
    canvas.drawPath(body, fillPaint)
    canvas.drawPath(body, strokePaint)
    return bitmap
}

internal fun causeIconId(marker: GlobeMarker, selected: Boolean): String {
    val suffix = if (selected) "-sel" else ""
    return when {
        marker.isFlight -> if (selected) PLANE_SELECTED else PLANE_NORMAL
        marker.isSatellite -> if (selected) SAT_SELECTED else SAT_NORMAL
        marker.layer == RadarLayer.SEISMIC -> "cause-seismic$suffix"
        marker.layer == RadarLayer.FIRES -> "cause-fire$suffix"
        marker.layer == RadarLayer.WEATHER -> "cause-weather$suffix"
        marker.layer == RadarLayer.MARITIME -> "cause-maritime$suffix"
        marker.layer == RadarLayer.MILITARY -> "cause-military$suffix"
        marker.layer == RadarLayer.CONFLICTS -> "cause-conflict$suffix"
        marker.layer == RadarLayer.OUTAGES -> "cause-outage$suffix"
        marker.layer == RadarLayer.CYBER -> "cause-cyber$suffix"
        marker.layer == RadarLayer.HAZARDS -> "cause-hazard$suffix"
        marker.layer == RadarLayer.NUCLEAR -> "cause-nuclear$suffix"
        marker.layer == RadarLayer.DATACENTERS -> "cause-datacenter$suffix"
        marker.layer == RadarLayer.UNREST -> "cause-unrest$suffix"
        else -> ""
    }
}

internal fun createSeismicBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(90, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, haloPaint)
    }
    val wave1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        alpha = 110
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * scale
    }
    val wave2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        alpha = 200
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * scale
    }
    val center = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    canvas.drawCircle(c, c, 13f * scale, wave1)
    canvas.drawCircle(c, c, 8.5f * scale, wave2)
    canvas.drawCircle(c, c, 4.5f * scale, center)
    canvas.drawCircle(c, c, 4.5f * scale, stroke)
    return bitmap
}

internal fun createFireBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val flame = Path().apply {
        moveTo(16f * scale, 3f * scale)
        cubicTo(21f * scale, 9f * scale, 27f * scale, 15f * scale, 26f * scale, 23f * scale)
        cubicTo(25f * scale, 29f * scale, 7f * scale, 29f * scale, 6f * scale, 23f * scale)
        cubicTo(5f * scale, 15f * scale, 11f * scale, 9f * scale, 16f * scale, 3f * scale)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 255, 85, 0)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * scale
    }
    val ember = Path().apply {
        moveTo(16f * scale, 11f * scale)
        cubicTo(19f * scale, 15f * scale, 21f * scale, 19f * scale, 20f * scale, 23f * scale)
        cubicTo(19f * scale, 25f * scale, 13f * scale, 25f * scale, 12f * scale, 23f * scale)
        cubicTo(11f * scale, 19f * scale, 13f * scale, 15f * scale, 16f * scale, 11f * scale)
        close()
    }
    val emberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 255, 209, 102)
        style = Paint.Style.FILL
    }
    canvas.drawPath(flame, fillPaint)
    canvas.drawPath(flame, strokePaint)
    canvas.drawPath(ember, emberPaint)
    return bitmap
}

internal fun createWeatherBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.STROKE
        strokeWidth = 2.4f * scale
        strokeCap = Paint.Cap.ROUND
    }
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scale
    }
    val spiral = Path().apply {
        moveTo(5f * scale, 16f * scale)
        cubicTo(5f * scale, 7f * scale, 13f * scale, 4f * scale, 19f * scale, 4f * scale)
        moveTo(27f * scale, 16f * scale)
        cubicTo(27f * scale, 25f * scale, 19f * scale, 28f * scale, 13f * scale, 28f * scale)
        moveTo(16f * scale, 4f * scale)
        cubicTo(25f * scale, 4f * scale, 28f * scale, 13f * scale, 28f * scale, 19f * scale)
    }
    canvas.drawPath(spiral, armPaint)
    canvas.drawCircle(c, c, 3.5f * scale, corePaint)
    canvas.drawCircle(c, c, 3.5f * scale, strokePaint)
    return bitmap
}

internal fun createMaritimeBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val hull = Path().apply {
        moveTo(16f * scale, 4f * scale)
        lineTo(22f * scale, 13f * scale)
        lineTo(22f * scale, 25f * scale)
        lineTo(10f * scale, 25f * scale)
        lineTo(10f * scale, 13f * scale)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    val cabinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawPath(hull, fillPaint)
    canvas.drawPath(hull, strokePaint)
    canvas.drawRect(13.5f * scale, 15f * scale, 18.5f * scale, 20f * scale, cabinPaint)
    canvas.drawRect(13.5f * scale, 15f * scale, 18.5f * scale, 20f * scale, strokePaint)
    return bitmap
}

internal fun createMilitaryBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scale
    }
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * scale
        strokeCap = Paint.Cap.ROUND
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, 9f * scale, reticlePaint)
    canvas.drawCircle(c, c, 2.5f * scale, dotPaint)
    canvas.drawLine(c, 2f * scale, c, 7f * scale, tickPaint)
    canvas.drawLine(c, 25f * scale, c, 30f * scale, tickPaint)
    canvas.drawLine(2f * scale, c, 7f * scale, c, tickPaint)
    canvas.drawLine(25f * scale, c, 30f * scale, c, tickPaint)
    return bitmap
}

internal fun createConflictBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val blast = Path().apply {
        moveTo(c, 2f * scale); lineTo((16f + 3f) * scale, (16f - 4f) * scale)
        lineTo((16f + 12f) * scale, (16f - 7f) * scale); lineTo((16f + 5f) * scale, c)
        lineTo((16f + 13f) * scale, (16f + 7f) * scale); lineTo((16f + 3f) * scale, (16f + 5f) * scale)
        lineTo(c, 30f * scale); lineTo((16f - 3f) * scale, (16f + 5f) * scale)
        lineTo((16f - 13f) * scale, (16f + 7f) * scale); lineTo((16f - 5f) * scale, c)
        lineTo((16f - 12f) * scale, (16f - 7f) * scale); lineTo((16f - 3f) * scale, (16f - 4f) * scale)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 255, 69, 58)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 255, 209, 102)
        style = Paint.Style.FILL
    }
    canvas.drawPath(blast, fillPaint)
    canvas.drawPath(blast, strokePaint)
    canvas.drawCircle(c, c, 3.5f * scale, corePaint)
    return bitmap
}

internal fun createOutageBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val bolt = Path().apply {
        moveTo(17f * scale, 2f * scale)
        lineTo(11f * scale, 15f * scale)
        lineTo(16f * scale, 16f * scale)
        lineTo(14f * scale, 29f * scale)
        lineTo(22f * scale, 15f * scale)
        lineTo(17f * scale, 15f * scale)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 255, 179, 0)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    canvas.drawPath(bolt, fillPaint)
    canvas.drawPath(bolt, strokePaint)
    return bitmap
}

internal fun createCyberBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val shield = Path().apply {
        moveTo(c, 4f * scale)
        lineTo(25f * scale, 10f * scale)
        lineTo(25f * scale, 18f * scale)
        cubicTo(25f * scale, 24f * scale, c, 29f * scale, c, 29f * scale)
        cubicTo(c, 29f * scale, 7f * scale, 24f * scale, 7f * scale, 18f * scale)
        lineTo(7f * scale, 10f * scale)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 0, 229, 255)
        style = Paint.Style.FILL
    }
    canvas.drawPath(shield, fillPaint)
    canvas.drawPath(shield, strokePaint)
    canvas.drawCircle(c, 15f * scale, 3.5f * scale, corePaint)
    return bitmap
}

internal fun createHazardBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val tri = Path().apply {
        moveTo(c, 3f * scale)
        lineTo(28f * scale, 25f * scale)
        lineTo(4f * scale, 25f * scale)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * scale
    }
    val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(240, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * scale
        strokeCap = Paint.Cap.ROUND
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(240, 5, 10, 15)
        style = Paint.Style.FILL
    }
    canvas.drawPath(tri, fillPaint)
    canvas.drawPath(tri, strokePaint)
    canvas.drawLine(c, 12f * scale, c, 18f * scale, markPaint)
    canvas.drawCircle(c, 22f * scale, 1.4f * scale, dotPaint)
    return bitmap
}

internal fun createNuclearBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    canvas.drawCircle(c, c, 3.5f * scale, fillPaint)
    for (angle in listOf(0.0, 120.0, 240.0)) {
        val rad = Math.toRadians(angle)
        val dx = (Math.sin(rad) * 8.0 * scale).toFloat()
        val dy = (-Math.cos(rad) * 8.0 * scale).toFloat()
        canvas.drawCircle(c + dx, c + dy, 4f * scale, fillPaint)
        canvas.drawCircle(c + dx, c + dy, 4f * scale, ringPaint)
    }
    canvas.drawCircle(c, c, 14f * scale, ringPaint)
    return bitmap
}

internal fun createDatacenterBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * scale
    }
    val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 81, 230, 186)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(8f * scale, 5f * scale, 24f * scale, 27f * scale, 3f * scale, 3f * scale, fillPaint)
    canvas.drawRoundRect(8f * scale, 5f * scale, 24f * scale, 27f * scale, 3f * scale, 3f * scale, strokePaint)
    canvas.drawLine(11f * scale, 11f * scale, 18f * scale, 11f * scale, linePaint)
    canvas.drawLine(11f * scale, 16f * scale, 18f * scale, 16f * scale, linePaint)
    canvas.drawLine(11f * scale, 21f * scale, 18f * scale, 21f * scale, linePaint)
    canvas.drawCircle(21f * scale, 11f * scale, 1.3f * scale, ledPaint)
    canvas.drawCircle(21f * scale, 16f * scale, 1.3f * scale, ledPaint)
    canvas.drawCircle(21f * scale, 21f * scale, 1.3f * scale, ledPaint)
    return bitmap
}

internal fun createUnrestBitmap(fillColor: Int, isSelected: Boolean, sizePx: Int = 72): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = sizePx / 32f
    val c = 16f * scale
    if (isSelected) {
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 209, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        canvas.drawCircle(c, c, 14.5f * scale, halo)
    }
    val mega = Path().apply {
        moveTo(8f * scale, 12f * scale)
        lineTo(13f * scale, 12f * scale)
        lineTo(21f * scale, 7f * scale)
        lineTo(21f * scale, 25f * scale)
        lineTo(13f * scale, 20f * scale)
        lineTo(8f * scale, 20f * scale)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 5, 10, 15)
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scale
    }
    val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * scale
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(mega, fillPaint)
    canvas.drawPath(mega, strokePaint)
    canvas.drawArc(22f * scale, 9f * scale, 30f * scale, 23f * scale, -45f, 90f, false, wavePaint)
    return bitmap
}

private val iconCache = HashMap<String, String>()

/** PNG data URL for [name] drawn in [argb]; cached per name and colour. Empty when the marker has no icon. */
internal fun iconDataUrl(name: String, argb: Int): String {
    if (name.isEmpty()) return ""
    val selected = name.endsWith("-sel") || name.endsWith("-selected")
    val fill = if (selected) SELECTED_ARGB else argb
    val size = if (selected) 88 else 72
    return iconCache.getOrPut("$name/$fill") {
        val bitmap = when (name.removeSuffix("-sel").removeSuffix("-selected").removeSuffix("-normal")) {
            "plane" -> createAirplaneBitmap(fill, sizePx = size)
            "sat" -> createSatelliteBitmap(fill, sizePx = size)
            "cause-seismic" -> createSeismicBitmap(fill, selected, size)
            "cause-fire" -> createFireBitmap(fill, selected, size)
            "cause-weather" -> createWeatherBitmap(fill, selected, size)
            "cause-maritime" -> createMaritimeBitmap(fill, selected, size)
            "cause-military" -> createMilitaryBitmap(fill, selected, size)
            "cause-conflict" -> createConflictBitmap(fill, selected, size)
            "cause-outage" -> createOutageBitmap(fill, selected, size)
            "cause-cyber" -> createCyberBitmap(fill, selected, size)
            "cause-hazard" -> createHazardBitmap(fill, selected, size)
            "cause-nuclear" -> createNuclearBitmap(fill, selected, size)
            "cause-datacenter" -> createDatacenterBitmap(fill, selected, size)
            "cause-unrest" -> createUnrestBitmap(fill, selected, size)
            else -> return ""
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
