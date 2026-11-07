package com.taurotechnologies.godseye.nativeapp.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import kotlinx.coroutines.launch
import kotlin.math.max

/** Height of the sheet when it is only peeking: handle, title, count and live message. */
val STREAM_PEEK_HEIGHT: Dp = 96.dp

/** Gap kept between a fully open sheet and the top bar. */
private val FULL_TOP_GAP: Dp = 8.dp

/** Running total of one drag gesture, read when the drag stops. */
private class DragTotal {
    var px: Float = 0f
}

/**
 * Draggable list sheet. It snaps between [StreamSheet.PEEK], [StreamSheet.HALF] and [StreamSheet.FULL]; a drag
 * down past peek hides it. Only the handle and [header] drag it, so the list keeps its own scrolling.
 * Map touches pass through everywhere outside the sheet's own surface.
 */
@Composable
fun BoxScope.HomeStreamSheet(
    state: StreamSheet,
    onStateChange: (StreamSheet) -> Unit,
    maxHeight: Dp,
    topChrome: Dp,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val fullDp = max(STREAM_PEEK_HEIGHT.value, maxHeight.value - topChrome.value - FULL_TOP_GAP.value).dp
    fun targetDp(s: StreamSheet): Dp = when (s) {
        StreamSheet.HIDDEN -> 0.dp
        StreamSheet.PEEK -> STREAM_PEEK_HEIGHT
        StreamSheet.HALF -> maxHeight * 0.5f
        StreamSheet.FULL -> fullDp
    }
    val targetPx = with(density) { targetDp(state).toPx() }
    val fullPx = with(density) { fullDp.toPx() }
    val peekPx = with(density) { STREAM_PEEK_HEIGHT.toPx() }
    val height = remember { Animatable(targetPx) }
    LaunchedEffect(state, targetPx) { height.animateTo(targetPx) }

    val currentState by rememberUpdatedState(state)
    val currentFull by rememberUpdatedState(fullPx)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val drag = remember { DragTotal() }
    val draggable = rememberDraggableState { delta ->
        drag.px += delta
        scope.launch { height.snapTo((height.value - delta).coerceIn(0f, currentFull)) }
    }
    val settleTo: suspend (StreamSheet) -> Unit = { next ->
        currentOnStateChange(next)
        height.animateTo(with(density) { targetDp(next).toPx() })
    }

    val px = height.value
    if (px < 1f && state == StreamSheet.HIDDEN) return
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(with(density) { px.toDp() }),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = colors.cardSurface,
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = draggable,
                        orientation = Orientation.Vertical,
                        onDragStarted = { drag.px = 0f },
                        onDragStopped = { velocity ->
                            val next = settleStreamSheet(currentState, drag.px, velocity)
                            settleTo(next)
                        },
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .background(colors.borderLight, RoundedCornerShape(2.dp)),
                )
                header()
            }
            if (px > peekPx + 4f) {
                Box(Modifier.fillMaxWidth().weight(1f)) { body() }
            }
        }
    }
}
