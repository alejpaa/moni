package com.moni.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moni.app.R
import com.moni.app.ui.theme.MoniColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Isotipo: monkey face on a graphite rounded square (docs/brand/moni-isotipo.svg). */
@Composable
internal fun MoniMark(size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(R.drawable.moni_isotipo), contentDescription = null,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.22f)),
    )
}

/** Isotipo + "moni" wordmark (docs/design.md: 20-22dp isotipo + moni 700, -0.02em). */
@Composable
internal fun MoniLockup(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MoniMark(20.dp)
        Text(
            "moni",
            color = MoniColors.fg,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.32).sp,
            ),
        )
    }
}

/** Mascot M3: the monkey peeking over a gold coin. Enters with a springy peek (M2 Fluido). */
@Composable
internal fun MoniMascot(width: Dp = 104.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scale = remember(context) { systemAnimatorScale(context) }
    val progress = remember { Animatable(if (scale <= 0f) 1f else 0f) }
    val alpha = remember { Animatable(if (scale <= 0f) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (scale <= 0f) return@LaunchedEffect
        val d = scaledDelayMs(120, scale)
        if (d > 0) delay(d)
        launch { alpha.animateTo(1f, tween(300)) }
        progress.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }
    Image(
        painterResource(R.drawable.moni_mascota), contentDescription = null,
        modifier = modifier
            .size(width, width * 1.1f)
            .graphicsLayer {
                this.alpha = alpha.value
                val s = 0.85f + 0.15f * progress.value
                scaleX = s; scaleY = s
                translationY = (1f - progress.value) * 24.dp.toPx()
            },
    )
}
