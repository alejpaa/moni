package com.moni.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.moni.app.R

/** Design tokens, see docs/design.md (J4 Lavanda & Grafito, light only). */
object MoniColors {
    val bg = Color(0xFFF7F6F8)
    val fg = Color(0xFF1F1E22)
    val mut = Color(0xFF74727A)
    val card = Color(0xFFFFFFFF)
    val line = Color(0xFFE7E5EA)
    val divider = Color(0xFFF0EEF2)
    val hero = Color(0xFF2B2A2F)
    val heroFg = Color(0xFFD4CCF6)
    val acc = Color(0xFF9C8CD9)
    val accFg = Color(0xFFFFFFFF)
    val inc = Color(0xFF2F8F7A)
    val exp = Color(0xFFC2416B)
    val iconTile = Color(0xFFEFEBFB)
    val iconStroke = Color(0xFF6F5FC0)
    val newTint = Color(0xFFF4F1FD)
    val track = Color(0xFFEFEDF3)
}

// Static instances generated from the variable font: Android does not reliably
// apply wght variation settings to resource fonts, so every weight rendered as 400.
private val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: Int, lineHeight: Int, tracking: Float = 0f) =
    TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.sp,
    )

private val MoniTypography = Typography(
    displaySmall = style(32, 700, 38, -0.96f),
    headlineMedium = style(28, 700, 34, -0.56f),
    headlineSmall = style(22, 700, 28, -0.44f),
    titleLarge = style(22, 700, 28, -0.44f),
    titleMedium = style(16, 600, 22),
    titleSmall = style(14, 600, 20),
    bodyLarge = style(16, 400, 24),
    bodyMedium = style(14, 400, 20),
    bodySmall = style(12, 400, 16),
    labelLarge = style(14, 600, 20),
    labelMedium = style(12, 500, 16),
    labelSmall = style(10, 600, 14, 1.2f),
)

/** Tabular figures for amounts and dates. */
val TextStyle.tabular get() = copy(fontFeatureSettings = "tnum")

private val Scheme = lightColorScheme(
    primary = MoniColors.acc,
    onPrimary = MoniColors.accFg,
    secondary = MoniColors.acc,
    background = MoniColors.bg,
    onBackground = MoniColors.fg,
    surface = MoniColors.card,
    onSurface = MoniColors.fg,
    surfaceVariant = MoniColors.track,
    onSurfaceVariant = MoniColors.mut,
    surfaceContainerLow = MoniColors.card,
    surfaceContainer = MoniColors.card,
    surfaceContainerHigh = MoniColors.card,
    surfaceContainerHighest = MoniColors.card,
    outline = MoniColors.line,
    outlineVariant = MoniColors.line,
    error = MoniColors.exp,
    onError = Color.White,
)

@Composable
fun MoniTheme(content: @Composable () -> Unit) {
    // Light only for now: system dark mode is ignored (docs/design.md).
    MaterialTheme(colorScheme = Scheme, typography = MoniTypography, content = content)
}
