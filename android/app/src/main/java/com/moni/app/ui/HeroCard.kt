package com.moni.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Prefs
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular

/**
 * Balance card: shows a calm skeleton while [loaded] is false, counts up over ~900ms on the
 * first load of the session ([animateInitial]) using a 0..1 Double interpolation (exact final
 * value, no Float precision loss), and animates changes over 500ms thereafter.
 */
@Composable
internal fun HeroCard(
    balance: Double,
    income: Double,
    expense: Double,
    loaded: Boolean,
    month: String,
    animateInitial: Boolean,
    onInitialAnimated: () -> Unit,
) {
    val hideAmounts = LocalHideAmounts.current
    val balanceReveal = rememberRevealState(hideAmounts)
    val incomeReveal = rememberRevealState(hideAmounts)
    val expenseReveal = rememberRevealState(hideAmounts)
    val shape = RoundedCornerShape(22.dp)
    val skeletonTint = MoniColors.heroFg.copy(alpha = 0.16f)

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color(0x732B2A2F), spotColor = Color(0x732B2A2F))
            .clip(shape)
            .background(MoniColors.hero)
            .padding(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Balance · $month",
                color = MoniColors.heroFg.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { Prefs.setHideAmounts(!hideAmounts) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                LucideIcon(
                    if (hideAmounts) LucideR.drawable.lucide_ic_eye_off else LucideR.drawable.lucide_ic_eye,
                    if (hideAmounts) "Mostrar montos" else "Ocultar montos",
                    tint = MoniColors.heroFg.copy(alpha = 0.85f),
                    size = 18,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        if (!loaded) {
            SkeletonBox(Modifier.width(172.dp).height(38.dp), color = skeletonTint, radius = 10.dp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SkeletonBox(Modifier.width(88.dp).height(20.dp), color = skeletonTint, radius = 6.dp)
                SkeletonBox(Modifier.width(88.dp).height(20.dp), color = skeletonTint, radius = 6.dp)
            }
        } else {
            val progress = remember { Animatable(if (animateInitial) 0f else 1f) }
            var startBalance by remember { mutableDoubleStateOf(if (animateInitial) 0.0 else balance) }
            var startIncome by remember { mutableDoubleStateOf(if (animateInitial) 0.0 else income) }
            var startExpense by remember { mutableDoubleStateOf(if (animateInitial) 0.0 else expense) }
            var targetBalance by remember { mutableDoubleStateOf(balance) }
            var targetIncome by remember { mutableDoubleStateOf(income) }
            var targetExpense by remember { mutableDoubleStateOf(expense) }
            var initialDone by remember { mutableStateOf(!animateInitial) }

            val p = progress.value.toDouble().coerceIn(0.0, 1.0)
            val shownBalance = if (p >= 1.0) targetBalance else startBalance + (targetBalance - startBalance) * p
            val shownIncome = if (p >= 1.0) targetIncome else startIncome + (targetIncome - startIncome) * p
            val shownExpense = if (p >= 1.0) targetExpense else startExpense + (targetExpense - startExpense) * p

            LaunchedEffect(balance, income, expense) {
                val changed = targetBalance != balance || targetIncome != income || targetExpense != expense
                if (!initialDone || changed) {
                    val duration = if (!initialDone) 900 else 500
                    startBalance = shownBalance
                    startIncome = shownIncome
                    startExpense = shownExpense
                    targetBalance = balance
                    targetIncome = income
                    targetExpense = expense
                    progress.snapTo(0f)
                    progress.animateTo(1f, tween(duration, easing = EaseOutCubic))
                    if (!initialDone) {
                        initialDone = true
                        onInitialAnimated()
                    }
                }
            }

            Text(
                formatSoles(shownBalance, balanceReveal.effectiveHidden),
                color = MoniColors.heroFg,
                style = MaterialTheme.typography.displaySmall.tabular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.revealOnTap(balanceReveal, label = "Balance"),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                HeroStat(
                    LucideR.drawable.lucide_ic_arrow_up,
                    "Ingresos",
                    formatSoles(shownIncome, incomeReveal.effectiveHidden),
                    incomeReveal,
                )
                HeroStat(
                    LucideR.drawable.lucide_ic_arrow_down,
                    "Gastos",
                    formatSoles(shownExpense, expenseReveal.effectiveHidden),
                    expenseReveal,
                )
            }
        }
    }
}

@Composable
private fun HeroStat(icon: Int, label: String, value: String, reveal: RevealState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .revealOnTap(reveal, label = label)
            .semantics(mergeDescendants = true) {},
    ) {
        LucideIcon(icon, label, tint = MoniColors.heroFg.copy(alpha = 0.7f), size = 16)
        Spacer(Modifier.width(6.dp))
        Text(value, color = MoniColors.heroFg, style = MaterialTheme.typography.labelLarge.tabular)
    }
}
