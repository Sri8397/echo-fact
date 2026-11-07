package com.taurotechnologies.godseye.nativeapp.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.R
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors
import com.taurotechnologies.godseye.nativeapp.ui.theme.SuvidhaColors
import kotlin.math.cos
import kotlin.math.sin

private const val STEP_MILLIS = 650L
private val BOOT_STEPS = listOf(
    "Preparing your dashboard",
    "Loading map and panels",
    "Checking available sources",
    "Restoring your settings",
    "Data status is shown in each panel",
)

/**
 * Full-screen start-up view shown while the first live feeds load. It replaces the small
 * pull-to-refresh spinner that overlapped the header. A radar emblem with the GodsEye logo sits
 * over a slow sweep, and a step card ticks through as the boot runs. All colours come from the
 * app theme, so light and dark mode both match.
 */
@Composable
fun BootScreen(sourcesLoading: Boolean, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    val transition = rememberInfiniteTransition(label = "boot")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3800, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    // Only the first two steps are local. The rest finish when the live feeds have actually loaded,
    // so the bar no longer runs to 100% while the network is still busy.
    var completed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (completed < 2) {
            kotlinx.coroutines.delay(STEP_MILLIS)
            completed += 1
        }
    }
    LaunchedEffect(sourcesLoading) {
        if (!sourcesLoading) {
            while (completed < BOOT_STEPS.size) {
                kotlinx.coroutines.delay(200)
                completed += 1
            }
        }
    }
    val progress = completed.toFloat() / BOOT_STEPS.size
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")

    Box(modifier.fillMaxSize().background(colors.canvas)) {
        // Landscape is only ~360 dp tall: the emblem and the steps sit side by side instead of stacked, and
        // both halves scroll, so nothing (progress bar, Cancel) can be pushed off the bottom of the screen.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight
            val header: @Composable ColumnScope.() -> Unit = {
                // Radar emblem: rings, a rotating arm and the logo at the centre.
                Box(Modifier.padding(top = if (wide) 0.dp else 36.dp).size(if (wide) 150.dp else 200.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f
                        for (ring in 1..3) {
                            drawCircle(colors.green.copy(alpha = 0.18f), radius = radius * ring / 3f, center = center, style = Stroke(1.dp.toPx()))
                        }
                        val rad = Math.toRadians(sweep.toDouble())
                        drawLine(
                            colors.green,
                            center,
                            Offset(center.x + (radius * cos(rad)).toFloat(), center.y + (radius * sin(rad)).toFloat()),
                            strokeWidth = 2.dp.toPx(),
                        )
                        drawCircle(colors.green.copy(alpha = 0.25f * pulse), radius = radius * pulse, center = center, style = Stroke(2.dp.toPx()))
                    }
                    Image(
                        painter = painterResource(R.drawable.godseye_logo),
                        contentDescription = "GodsEye logo",
                        modifier = Modifier.size(104.dp).clip(CircleShape),
                    )
                }

                Text(
                    "GodsEye World Radar",
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    "Global intelligence system · v2.0",
                    color = colors.muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )

            }
            val body: @Composable ColumnScope.() -> Unit = {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = if (wide) 0.dp else 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderLight),
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        BOOT_STEPS.forEachIndexed { index, label ->
                            BootStepRow(
                                label = label,
                                done = index < completed,
                                active = index == completed,
                            )
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("INITIALIZING", color = colors.muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            color = colors.green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = colors.green,
                        trackColor = colors.borderLight,
                    )
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Cancel", color = colors.muted, fontSize = 13.sp)
                }
            }
            val frame = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)
            if (wide) {
                Row(frame, horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = header,
                    )
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = body,
                    )
                }
            } else {
                Column(
                    frame.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    header()
                    body()
                }
            }
        }
    }
}

@Composable
private fun BootStepRow(label: String, done: Boolean, active: Boolean) {
    val colors: SuvidhaColors = LocalSuvidhaColors.current
    val alpha by animateFloatAsState(
        targetValue = if (done || active) 1f else 0.45f,
        animationSpec = tween(300),
        label = "stepAlpha",
    )
    Row(
        Modifier.fillMaxWidth().alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when {
                done -> Box(Modifier.size(22.dp).background(colors.green, CircleShape), contentAlignment = Alignment.Center) {
                    Text("✓", color = colors.onGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                active -> CircularProgressIndicator(Modifier.size(18.dp), color = colors.green, strokeWidth = 2.dp)
                else -> Box(Modifier.size(18.dp).border(1.5.dp, colors.borderLight, CircleShape))
            }
        }
        Text(
            label,
            color = if (done || active) colors.ink else colors.muted,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
