package com.just_sri.suvidha.radar.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_sri.suvidha.radar.Toast
import com.just_sri.suvidha.radar.ui.theme.LocalSuvidhaColors
import kotlinx.coroutines.delay

val CardShape: RoundedCornerShape = RoundedCornerShape(16.dp)
private const val TOAST_MILLIS: Long = 3500

/** Only http(s) links are opened; anything else (javascript:, file:, intent:) is ignored. */
fun isWebUrl(url: String): Boolean {
    val clean: String = url.trim().lowercase()
    return clean.startsWith("https://") || clean.startsWith("http://")
}

fun openWebUrl(context: Context, url: String) {
    if (!isWebUrl(url)) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No browser installed: nothing to open.
    }
}

/** Suvidha's Panel: one bordered card that holds a flat list of rows for a single section. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalSuvidhaColors.current
    Surface(shape = CardShape, color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderLight), modifier = modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

/**
 * One row inside a [Panel] (Suvidha's LedgerRow layout: leading, text column, trailing, inset divider).
 * [divider] draws the line under the row; pass false for the last row.
 */
@Composable
fun PanelRow(
    divider: Boolean,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
            trailing?.invoke()
        }
        if (divider) HorizontalDivider(color = colors.borderLight, modifier = Modifier.padding(start = if (leading != null) 44.dp else 16.dp))
    }
}

/** Entry card: 16dp corners, 1dp light border (Suvidha card rule). */
@Composable
fun EntryCard(
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    val clickable: Modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        shape = CardShape,
        color = if (isHighlighted) colors.mint else colors.cardSurface,
        border = BorderStroke(1.dp, if (isHighlighted) colors.green else colors.borderLight),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = clickable.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
fun SelectChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    val background: Color = if (isSelected) colors.mint else colors.cardSurface
    val border: Color = if (isSelected) colors.green else colors.borderLight
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) colors.green else colors.muted)
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** One short hint line under a heading or field. Every screen uses this, so hints stay the same size. */
@Composable
fun HelpText(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 16.sp, color = LocalSuvidhaColors.current.muted)
}

@Composable
fun SectionLabel(text: String, color: Color = LocalSuvidhaColors.current.green) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = color, letterSpacing = 0.8.sp)
}

/** Primary action: green for normal actions, [isDanger] red for destructive ones. Shows a spinner while [isBusy]. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, isBusy: Boolean = false, isDanger: Boolean = false, isEnabled: Boolean = true, modifier: Modifier = Modifier) {
    val colors = LocalSuvidhaColors.current
    val container: Color = if (isDanger) colors.danger else colors.green
    Button(
        onClick = onClick,
        enabled = isEnabled && !isBusy,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = colors.canvas),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isBusy) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = colors.canvas, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/** Every popup is a bottom sheet (Suvidha rule: no centred dialogs). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalSuvidhaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.canvas,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Confirm sheet: the two buttons split 50/50. */
@Composable
fun ConfirmSheet(title: String, body: String, confirmText: String, isBusy: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalSuvidhaColors.current
    AppSheet(onDismiss = onDismiss) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        Text(body, fontSize = 13.sp, color = colors.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, enabled = !isBusy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Text("Cancel", color = colors.ink)
            }
            PrimaryButton(confirmText, onConfirm, isBusy = isBusy, isDanger = true, modifier = Modifier.weight(1f))
        }
    }
}

/** Blue for success, red for failure. Auto-hides. */
@Composable
fun AppToast(toast: Toast?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (toast == null) return
    val colors = LocalSuvidhaColors.current
    LaunchedEffect(toast) {
        delay(TOAST_MILLIS)
        onDismiss()
    }
    val background: Color = if (toast.isSuccess) colors.confirm else colors.danger
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(background, RoundedCornerShape(14.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(if (toast.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, contentDescription = null, tint = colors.canvas)
        Text(toast.text, color = colors.canvas, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
