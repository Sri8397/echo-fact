package com.taurotechnologies.godseye.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Pill = Color(0xE6101418)
private val Glyph = Color(0xFFEDEDED)

/**
 * Camera controls for the globe: zoom, pan, rotate, tilt, reset. Collapsed by default to a single
 * compass button (tap = face north) and a toggle, so the map stays uncluttered; the toggle opens the
 * full bar, and the last button closes it. Holding a button repeats the move.
 */
@Composable
fun MapNavControls(
    headingDeg: Float,
    flat: Boolean,
    tiltAvailable: Boolean,
    onCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val send by rememberUpdatedState(onCommand)

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (open) {
            Row(
                Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Group("Zoom", Icons.Filled.Add to "zoomIn", Icons.Filled.Remove to "zoomOut", send = send)
                Group("Pan", Icons.Filled.KeyboardArrowLeft to "left", Icons.Filled.KeyboardArrowRight to "right", send = send)
                Group("Move", Icons.Filled.KeyboardArrowUp to "forward", Icons.Filled.KeyboardArrowDown to "back", send = send)
                Group("Rotate", Icons.Outlined.RotateLeft to "rotateCcw", Icons.Outlined.RotateRight to "rotateCw", enabled = !flat, send = send)
                if (tiltAvailable) Group("Tilt", Icons.Outlined.ArrowCircleUp to "tiltUp", Icons.Outlined.ArrowCircleDown to "tiltDown", enabled = !flat, send = send)
            }
            RoundButton("Hide map controls", Icons.Outlined.CloseFullscreen) { open = false }
        } else {
            RoundButton("Show map controls", Icons.Outlined.Tune) { open = true }
        }
        // Compass: needle follows the heading; tap to face north, long-press to reset the tilt too.
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Pill)
                .semantics { contentDescription = "Compass, tap to face north" }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { send("north") }, onLongPress = { send("reset") })
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Explore, null, tint = Color(0xFFFF5A4F), modifier = Modifier.size(24.dp).rotate(-headingDeg))
        }
    }
}

@Composable
private fun Group(
    label: String,
    first: Pair<ImageVector, String>,
    second: Pair<ImageVector, String>,
    enabled: Boolean = true,
    send: (String) -> Unit,
) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Pill).alpha(if (enabled) 1f else 0.35f),
    ) {
        for ((icon, cmd) in listOf(first, second)) {
            HoldButton("$label ${cmd}", icon, enabled) { send(cmd) }
        }
    }
}

@Composable
private fun HoldButton(
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    round: Boolean = false,
    action: () -> Unit,
) {
    val current by rememberUpdatedState(action)
    val theme = com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors.current
    // Round buttons match the map's float buttons (44 dp, card surface, shadow) so the side column lines up.
    Box(
        Modifier
            .size(if (round) 44.dp else 40.dp)
            .then(
                if (round) Modifier.alpha(if (enabled) 1f else 0.35f).shadow(4.dp, CircleShape).clip(CircleShape).background(theme.cardSurface)
                else Modifier,
            )
            .semantics { contentDescription = description }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    current()
                    coroutineScope {
                        val repeat = launch {
                            delay(350)
                            while (true) {
                                current()
                                delay(90)
                            }
                        }
                        tryAwaitRelease()
                        repeat.cancel()
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (round) theme.ink else Glyph, modifier = Modifier.size(if (round) 20.dp else 22.dp))
    }
}

/**
 * Camera readout for the strip along the bottom of the map: height above ground and, in 3D, tilt.
 * Plain text with a soft shadow (no pill) so it reads on light and dark imagery. Values are read here
 * only, so camera moves recompose this line and nothing else.
 */
@Composable
fun MapCameraReadout(
    heightMeters: () -> Double, tiltPitch: (() -> Float)?,
    lat: () -> Double, lng: () -> Double, heading: () -> Float,
    modifier: Modifier = Modifier,
) {
    val text = buildString {
        append(formatCameraLatLng(lat(), lng()))
        append("  ·  ").append(formatElevation(heightMeters()))
        if (tiltPitch != null) append("  ·  ").append(formatTilt(tiltPitch()))
        append("  ·  ").append(formatHeading(heading()))
    }
    Text(
        text, modifier = modifier, maxLines = 1, softWrap = false,
        style = androidx.compose.ui.text.TextStyle(
            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.85f), blurRadius = 4f),
        ),
    )
}

@Composable
private fun RoundButton(description: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Pill)
            .semantics { contentDescription = description }
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Glyph, modifier = Modifier.size(20.dp))
    }
}

/**
 * Tilt up/down for the 3D view on the normal map (the full bar is only in full screen). Two stacked
 * round buttons, the same style as the other map buttons, with no enclosing box.
 */
@Composable
fun MapTiltPill(flat: Boolean, onCommand: (String) -> Unit, modifier: Modifier = Modifier) {
    val send by rememberUpdatedState(onCommand)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HoldButton("Tilt tiltUp", Icons.Outlined.ArrowCircleUp, enabled = !flat, round = true) { send("tiltUp") }
        HoldButton("Tilt tiltDown", Icons.Outlined.ArrowCircleDown, enabled = !flat, round = true) { send("tiltDown") }
    }
}

/**
 * Zoom in/out with the camera's height between them, for the column beside the selected-item card.
 * Same round hold-to-repeat buttons as [MapTiltPill]. [heightMeters] is read here only, so camera moves
 * recompose this pill and nothing else.
 */
@Composable
fun MapZoomPill(onCommand: (String) -> Unit, modifier: Modifier = Modifier) {
    val send by rememberUpdatedState(onCommand)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HoldButton("Zoom in", Icons.Filled.Add, enabled = true, round = true) { send("zoomIn") }
        HoldButton("Zoom out", Icons.Filled.Remove, enabled = true, round = true) { send("zoomOut") }
    }
}

/** Tilt from straight down: 0° looks straight at the ground, 90° at the horizon. [pitch] is Cesium's (-90 = down). */
internal fun formatCameraLatLng(lat: Double, lng: Double): String =
    String.format(java.util.Locale.ROOT, "%.4f°%s %.4f°%s", kotlin.math.abs(lat), if (lat >= 0) "N" else "S", kotlin.math.abs(lng), if (lng >= 0) "E" else "W")

internal fun formatHeading(deg: Float): String = "${((deg % 360f) + 360f).toInt() % 360}° hdg"

internal fun formatTilt(pitch: Float): String = "${(pitch + 90f).coerceIn(0f, 90f).toInt()}° tilt"

/** Camera height above the ground: metres up close, kilometres beyond 10 km. */
internal fun formatElevation(meters: Double): String = when {
    meters <= 0.0 || meters.isNaN() -> "—"
    meters < 10_000 -> "${meters.toInt()} m"
    else -> String.format(java.util.Locale.ROOT, "%,d km", (meters / 1000).toLong())
}

/** The control bar on its own (zoom, pan, move, rotate, tilt), for screens that draw their own compass and toggle. */
@Composable
fun MapNavBar(flat: Boolean, tiltAvailable: Boolean, onCommand: (String) -> Unit, modifier: Modifier = Modifier) {
    val send by rememberUpdatedState(onCommand)
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Group("Zoom", Icons.Filled.Add to "zoomIn", Icons.Filled.Remove to "zoomOut", send = send)
        Group("Pan", Icons.Filled.KeyboardArrowLeft to "left", Icons.Filled.KeyboardArrowRight to "right", send = send)
        Group("Move", Icons.Filled.KeyboardArrowUp to "forward", Icons.Filled.KeyboardArrowDown to "back", send = send)
        Group("Rotate", Icons.Outlined.RotateLeft to "rotateCcw", Icons.Outlined.RotateRight to "rotateCw", enabled = !flat, send = send)
        if (tiltAvailable) Group("Tilt", Icons.Outlined.ArrowCircleUp to "tiltUp", Icons.Outlined.ArrowCircleDown to "tiltDown", enabled = !flat, send = send)
    }
}

/**
 * Previous / next for the selected map item, stacked tightly: up arrow, place in the list ("3/500"), down arrow.
 * It is as tall as the card beside it (the caller stretches both to the row's height) and its items are centred
 * in that height, so the arrows and count read as one control. Tapping the count opens the full list.
 */
@Composable
fun MapStepper(
    position: Int,
    total: Int,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onOpenList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors.current
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxHeight(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 30),
        color = theme.cardSurface,
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier.width(44.dp).fillMaxHeight().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StepArrow(Icons.Filled.KeyboardArrowUp, "Previous", onPrevious, theme.ink, theme.muted)
            androidx.compose.material3.Text(
                "$position/$total",
                color = theme.green,
                fontSize = 10.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.clickable(onClickLabel = "Open the full list", onClick = onOpenList),
            )
            StepArrow(Icons.Filled.KeyboardArrowDown, "Next", onNext, theme.ink, theme.muted)
        }
    }
}

@Composable
private fun StepArrow(icon: ImageVector, label: String, onClick: (() -> Unit)?, ink: Color, muted: Color) {
    Box(
        Modifier.size(26.dp)
            .semantics { contentDescription = label }
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (onClick != null) ink else muted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
    }
}
