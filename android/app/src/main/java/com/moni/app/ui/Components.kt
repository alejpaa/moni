package com.moni.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.ui.theme.MoniColors
import kotlinx.coroutines.delay

/** Soft spring from docs/design.md (M2 Fluido). */
fun <T> moniSpring() = spring<T>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

/** Scale 0.94 while pressed (90ms), springs back with a bounce. */
@Composable
fun Modifier.pressScale(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = if (pressed) tween(90) else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Fade + rise + scale entrance, staggered with [delayMs]. */
@Composable
fun Entrance(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    initialScale: Float = 0.96f,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(520, delayMs)) +
            slideInVertically(tween(520, delayMs)) { it / 6 } +
            scaleIn(tween(520, delayMs), initialScale = initialScale),
    ) { content() }
}

@Composable
fun LucideIcon(
    res: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MoniColors.fg,
    size: Int = 18,
) {
    Icon(painterResource(res), contentDescription, modifier.size(size.dp), tint)
}

/** Primary lavender button: radius 18, tinted shadow, press spring, optional spinner. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: Int? = null,
) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    val bg = if (enabled) MoniColors.acc else MoniColors.acc.copy(alpha = 0.55f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            .shadow(
                if (enabled) 12.dp else 0.dp, shape,
                ambientColor = MoniColors.acc.copy(alpha = 0.8f),
                spotColor = MoniColors.acc.copy(alpha = 0.8f),
            )
            .clip(shape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = source, indication = androidx.compose.material3.ripple()) { onClick() }
            .height(52.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = MoniColors.accFg, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            } else if (icon != null) {
                LucideIcon(icon, null, tint = MoniColors.accFg)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = MoniColors.accFg, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

/** Segmented control with a spring-animated indicator. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MoniColors.track)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, moniSpring(), label = "segment")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segment)
                .height(48.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                .background(MoniColors.card, RoundedCornerShape(12.dp)),
        )
        Row {
            options.forEachIndexed { i, label ->
                val color by animateColorAsState(if (i == selected) MoniColors.fg else MoniColors.mut, label = "segText")
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = i == selected, role = Role.RadioButton) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = color, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Error (or info) banner; the wrapper is a live region so screen readers announce changes. */
@Composable
fun AlertBanner(message: String?, isError: Boolean = true, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf("") }
    if (message != null) last = message
    Box(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
            exit = fadeOut(tween(150)),
        ) {
            val tone = if (isError) MoniColors.exp else MoniColors.iconStroke
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(LucideR.drawable.lucide_ic_circle_alert, null, tint = tone)
                Spacer(Modifier.width(10.dp))
                Text(last, color = tone, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Inline field error: circle-alert icon + message in the expense color. */
@Composable
fun FieldMessage(text: String, isError: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        if (isError) {
            LucideIcon(LucideR.drawable.lucide_ic_circle_alert, null, tint = MoniColors.exp, size = 16, modifier = Modifier.padding(top = 1.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = if (isError) MoniColors.exp else MoniColors.mut, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun moniFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MoniColors.card,
    unfocusedContainerColor = MoniColors.card,
    errorContainerColor = MoniColors.card,
    focusedBorderColor = MoniColors.acc,
    unfocusedBorderColor = MoniColors.line,
    errorBorderColor = MoniColors.exp,
    focusedLabelColor = MoniColors.iconStroke,
    unfocusedLabelColor = MoniColors.mut,
    errorLabelColor = MoniColors.exp,
    cursorColor = MoniColors.acc,
    errorCursorColor = MoniColors.exp,
    focusedTextColor = MoniColors.fg,
    unfocusedTextColor = MoniColors.fg,
    errorTextColor = MoniColors.fg,
    focusedLeadingIconColor = MoniColors.iconStroke,
    unfocusedLeadingIconColor = MoniColors.mut,
    errorLeadingIconColor = MoniColors.exp,
    focusedTrailingIconColor = MoniColors.mut,
    unfocusedTrailingIconColor = MoniColors.mut,
    errorTrailingIconColor = MoniColors.mut,
)

val FieldShape = RoundedCornerShape(14.dp)
