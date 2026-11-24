package com.moni.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.moni.app.data.LIMA_ZONE
import com.moni.app.data.parseInstantOrNull
import com.moni.app.data.round2
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
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
private val MAX_AMOUNT_DECIMAL = BigDecimal("9999999999.99")
private val MIN_POSITIVE_DECIMAL = BigDecimal("0.01")
private val MONEY_REGEX = Regex("^-?\\d+(\\.\\d+)?$")

/** Global preference indicating whether monetary amounts should be masked. */
internal val LocalHideAmounts = compositionLocalOf { false }

/** Increments on ON_STOP so all temporarily revealed amounts re-mask immediately. */
internal val LocalRevealEpoch = compositionLocalOf { 0 }

private fun cleanZero(amount: Double): Double {
    val r = round2(amount)
    return if (r == 0.0) 0.0 else r
}

/** Formats [amount] with "S/ " prefix (and "-" when negative), or [MASKED_AMOUNT] when [hidden]. */
internal fun formatSoles(amount: Double, hidden: Boolean = false): String {
    if (hidden) return MASKED_AMOUNT
    val v = cleanZero(amount)
    return (if (v < 0) "-S/ " else "S/ ") + money.format(abs(v))
}

/** Consistent alias using the "S/ " prefix everywhere. */
internal fun formatMoney(amount: Double, hidden: Boolean = false): String =
    formatSoles(amount, hidden)

/** Formats a signed transaction amount ("+S/ " / "−S/ " + number), or [MASKED_AMOUNT] when [hidden]. */
internal fun formatSignedMoney(amount: Double, isIncome: Boolean, hidden: Boolean = false): String {
    if (hidden) return MASKED_AMOUNT
    val v = abs(cleanZero(amount))
    return (if (isIncome) "+S/ " else "−S/ ") + money.format(v)
}

internal fun parseMoneyDecimal(raw: String): BigDecimal? {
    val s = raw.trim().replace(',', '.')
    if (s.isEmpty() || !MONEY_REGEX.matches(s)) return null
    return s.toBigDecimalOrNull()
}

internal fun parseMoney(raw: String): Double? = parseMoneyDecimal(raw)?.toDouble()

internal fun plainDecimal(v: Double?): String =
    if (v == null) "" else BigDecimal.valueOf(cleanZero(v)).stripTrailingZeros().toPlainString()

/**
 * Validates a PEN numeric(12,2) amount string with max 2 decimals and Spanish inline errors.
 */
internal fun validateAmount(
    raw: String,
    allowNegative: Boolean = false,
    allowZero: Boolean = false,
    allowBlankAsZero: Boolean = false,
): Pair<Double?, String?> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        return if (allowBlankAsZero) 0.0 to null else null to "Introduce un monto"
    }
    val bd = parseMoneyDecimal(trimmed) ?: return null to "Introduce un monto válido"
    if (bd.scale() > 2) return null to "Máximo 2 decimales"
    if (bd.abs() > MAX_AMOUNT_DECIMAL) return null to "El monto máximo es S/ 9.999.999.999,99"
    if (!allowNegative && bd < BigDecimal.ZERO) return null to "El monto no puede ser negativo"
    val isZero = bd.compareTo(BigDecimal.ZERO) == 0
    if (!allowZero && isZero) return null to "El monto debe ser mayor que 0"
    if (!allowNegative && !allowZero && bd < MIN_POSITIVE_DECIMAL) {
        return null to "El monto mínimo es S/ 0,01"
    }
    return bd.setScale(2, RoundingMode.HALF_UP).toDouble() to null
}

/**
 * Holds temporary reveal state for a masked amount: tapping reveals it and
 * automatically re-hides it after ~5 seconds or when the app goes to background.
 */
internal class RevealState(
    val effectiveHidden: Boolean,
    val globalHidden: Boolean,
    val toggle: () -> Unit,
)

@Composable
internal fun rememberRevealState(globalHidden: Boolean = LocalHideAmounts.current): RevealState {
    val revealEpoch = LocalRevealEpoch.current
    var revealed by remember(revealEpoch) { mutableStateOf(false) }
    var nonce by remember(revealEpoch) { mutableIntStateOf(0) }
    LaunchedEffect(globalHidden, nonce, revealEpoch) {
        if (!globalHidden) {
            revealed = false
        } else if (revealed) {
            delay(REVEAL_DURATION_MS)
            revealed = false
        }
    }
    val effectiveHidden = globalHidden && !revealed
    return remember(effectiveHidden, globalHidden, revealEpoch) {
        RevealState(
            effectiveHidden = effectiveHidden,
            globalHidden = globalHidden,
            toggle = {
                if (globalHidden) {
                    revealed = !revealed
                    nonce++
                }
            },
        )
    }
}

/**
 * Makes a masked amount clickable (with >= 48dp touch target) to temporarily reveal it.
 * No-op when [state.globalHidden] is false.
 */
internal fun Modifier.revealOnTap(state: RevealState, label: String? = null): Modifier =
    if (!state.globalHidden) {
        this
    } else {
        this
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = if (state.effectiveHidden) "Mostrar monto" else "Ocultar monto",
                onClick = state.toggle,
            )
            .semantics {
                if (state.effectiveHidden) {
                    contentDescription = if (label != null) "$label, monto oculto" else "Monto oculto"
                }
            }
    }

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", esLocale)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", esLocale)

/** "2026-11-22T10:00:00+00:00" -> "22 nov" (adds the year when not the current one). */
internal fun formatDate(iso: String): String {
    val date = if (iso.length == 10) {
        try {
            LocalDate.parse(iso)
        } catch (_: Exception) {
            return iso
        }
    } else {
        parseInstantOrNull(iso)?.atZone(LIMA_ZONE)?.toLocalDate()
            ?: try {
                LocalDate.parse(iso.take(10))
            } catch (_: Exception) {
                return iso.take(10)
            }
    }
    val f = if (date.year == LocalDate.now(LIMA_ZONE).year) dayMonth else dayMonthYear
    return f.format(date).replace(".", "")
}

internal fun systemAnimatorScale(context: Context): Float {
    if (!ValueAnimator.areAnimatorsEnabled()) return 0f
    return try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            .coerceAtLeast(0f)
    } catch (_: Exception) {
        1f
    }
}

internal fun scaledDelayMs(baseMs: Long, scale: Float): Long =
    if (scale <= 0f) 0L else (baseMs * scale).toLong().coerceAtLeast(0L)
