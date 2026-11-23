package com.moni.app.ui

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular
import kotlinx.coroutines.delay

/** Balance card: counts up over ~900ms on first load, 500ms on later changes. */
@Composable
internal fun HeroCard(balance: Double, income: Double, expense: Double, loaded: Boolean) {
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(loaded) { if (loaded) { delay(1000); settled = true } }
    val shown by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(if (settled) 500 else 900, easing = EaseOutCubic),
        label = "balance",
    )
    val shownIncome by animateFloatAsState(income.toFloat(), tween(if (settled) 500 else 900, easing = EaseOutCubic), label = "inc")
    val shownExpense by animateFloatAsState(expense.toFloat(), tween(if (settled) 500 else 900, easing = EaseOutCubic), label = "exp")

    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color(0x732B2A2F), spotColor = Color(0x732B2A2F))
            .clip(shape)
            .background(MoniColors.hero)
            .padding(20.dp),
    ) {
        Text("Balance", color = MoniColors.heroFg.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            money.format(shown.toDouble()),
            color = MoniColors.heroFg,
            style = MaterialTheme.typography.displaySmall.tabular,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            HeroStat(LucideR.drawable.lucide_ic_arrow_up, "Ingresos", money.format(shownIncome.toDouble()))
            HeroStat(LucideR.drawable.lucide_ic_arrow_down, "Gastos", money.format(shownExpense.toDouble()))
        }
    }
}

@Composable
private fun HeroStat(icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LucideIcon(icon, label, tint = MoniColors.heroFg.copy(alpha = 0.7f), size = 16)
        Spacer(Modifier.width(6.dp))
        Text(value, color = MoniColors.heroFg, style = MaterialTheme.typography.labelLarge.tabular)
    }
}
