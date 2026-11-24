package com.moni.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val esLocale = Locale.forLanguageTag("es")

/** Spanish number format with exactly two decimals ("1.234,50"). PEN only. */
internal val money: NumberFormat = NumberFormat.getNumberInstance(esLocale).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

internal const val MASKED_AMOUNT = "S/ ••••"
private const val REVEAL_DURATION_MS = 5_000L

/** Global preference indicating whether monetary amounts should be masked. */
internal val LocalHideAmounts = compositionLocalOf { false }

/** Formats [amount] with two decimals, or returns [MASKED_AMOUNT] when [hidden] is true. */
internal fun formatMoney(amount: Double, hidden: Boolean = false): String =
    if (hidden) MASKED_AMOUNT else money.format(amount)

/** Formats [amount] with "S/ " prefix (and "-" when negative), or [MASKED_AMOUNT] when [hidden]. */
internal fun formatSoles(amount: Double, hidden: Boolean = false): String =
    if (hidden) MASKED_AMOUNT else (if (amount < 0) "-S/ " else "S/ ") + money.format(abs(amount))

/** Formats a signed transaction amount ("+" / "−" + number), or [MASKED_AMOUNT] when [hidden]. */
internal fun formatSignedMoney(amount: Double, isIncome: Boolean, hidden: Boolean = false): String =
    if (hidden) MASKED_AMOUNT else (if (isIncome) "+" else "−") + money.format(amount)

/**
 * Holds temporary reveal state for a masked amount: tapping reveals it and
 * automatically re-hides it after ~5 seconds.
 */
internal class RevealState(
    val effectiveHidden: Boolean,
    val globalHidden: Boolean,
    val toggle: () -> Unit,
)

@Composable
internal fun rememberRevealState(globalHidden: Boolean = LocalHideAmounts.current): RevealState {
    var revealed by remember { mutableStateOf(false) }
    var nonce by remember { mutableIntStateOf(0) }
    LaunchedEffect(globalHidden, nonce) {
        if (!globalHidden) {
            revealed = false
        } else if (revealed) {
            delay(REVEAL_DURATION_MS)
            revealed = false
        }
    }
    return RevealState(
        effectiveHidden = globalHidden && !revealed,
        globalHidden = globalHidden,
        toggle = {
            if (globalHidden) {
                revealed = !revealed
                nonce++
            }
        },
    )
}

/** Makes a masked amount clickable to temporarily reveal it. No-op when [state.globalHidden] is false. */
internal fun Modifier.revealOnTap(state: RevealState): Modifier =
    if (!state.globalHidden) {
        this
    } else {
        this
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = state.toggle)
            .semantics {
                contentDescription = if (state.effectiveHidden) "Monto oculto, toca para ver" else "Toca para ocultar monto"
            }
    }

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", esLocale)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", esLocale)

/** "2026-11-22T10:00:00+00:00" -> "22 nov" (adds the year when not the current one). */
internal fun formatDate(iso: String): String {
    val date = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }
        .recoverCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }
        .recoverCatching { LocalDate.parse(iso.take(10)) }
        .getOrNull() ?: return iso.take(10)
    val f = if (date.year == LocalDate.now().year) dayMonth else dayMonthYear
    return f.format(date).replace(".", "")
}
