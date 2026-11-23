package com.moni.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Account
import com.moni.app.data.Category
import com.moni.app.data.Reminder
import com.moni.app.data.Transaction
import com.moni.app.data.TxLite
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val cardShape = RoundedCornerShape(20.dp)
private val cardShadow = Color(0x339C8CD9)

@Composable
private fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .shadow(8.dp, cardShape, ambientColor = cardShadow, spotColor = cardShadow)
            .clip(cardShape)
            .background(MoniColors.card)
            .padding(16.dp),
    ) { content() }
}

private data class CatSlice(val key: String, val name: String, val icon: Int, val amount: Double, val pct: Int)

private fun monthLabel(): String {
    val d = LocalDate.now()
    val m = d.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
    return m.replaceFirstChar { it.uppercase() } + " " + d.year
}

private fun categorySlices(month: List<TxLite>, categories: List<Category>): List<CatSlice> {
    val expenses = month.filter { it.type == "expense" }
    val total = expenses.sumOf { it.amount }
    if (total <= 0.0) return emptyList()
    val byId = categories.associateBy { it.id }
    val sums = expenses.groupBy { it.categoryId }.map { (id, rows) -> id to rows.sumOf { it.amount } }
        .sortedByDescending { it.second }
    fun pct(a: Double) = Math.round(a / total * 100).toInt()
    val top = sums.take(5).map { (id, amount) ->
        val c = id?.let { byId[it] }
        CatSlice(id ?: "none", c?.name ?: "Sin categoría", categoryIcon(c, false), amount, pct(amount))
    }
    val rest = sums.drop(5).sumOf { it.second }
    return if (rest > 0) top + CatSlice("others", "Otros", LucideR.drawable.lucide_ic_arrow_up_right, rest, pct(rest)) else top
}

private fun List<Transaction>.toLite() = map { TxLite(it.type, it.amount, it.categoryId, it.accountId, it.occurredAt) }

private fun isThisMonth(iso: String, now: LocalDate): Boolean = iso.length >= 7 && iso.take(7) == "%04d-%02d".format(now.year, now.monthValue)

/**
 * "Inicio" tab. [onSeeAll] switches to Movimientos, [onOpenReview] opens the
 * pending-review sheet (hosted by AppShell).
 */
@Composable
fun DashboardScreen(
    state: UiState,
    contentPadding: PaddingValues,
    onSeeAll: () -> Unit,
    onOpenReview: () -> Unit,
) {
    val vm: DashboardViewModel = viewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    LaunchedEffect(state.transactions) { vm.refresh() }

    val now = LocalDate.now()
    // Fallback while loading: the latest 100 confirmed rows (approximate month view).
    val month = data?.month ?: remember(state.transactions) {
        state.transactions.filter { it.status == "confirmed" && isThisMonth(it.occurredAt, now) }.toLite()
    }
    val balances = data?.balances.orEmpty()

    val income = month.filter { it.type == "income" }.sumOf { it.amount }
    val expense = month.filter { it.type == "expense" }.sumOf { it.amount }
    val slices = remember(month, state.categories) { categorySlices(month, state.categories) }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val latest = remember(state.transactions) { state.transactions.take(5) }
    val upcoming = remember(state.reminders) { state.reminders.filter { it.frequency != null && !it.isDone }.take(5) }
    // Same figure as "Disponible" in Cuentas: money in non-card accounts.
    val totalBalance = state.accounts.filter { it.type != "credit_card" }.sumOf { balances[it.id] ?: it.openingBalance }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Entrance { ScreenHeader("moni") }
        }
        item {
            Entrance(delayMs = 70, initialScale = 0.92f) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeroCard(income - expense, income, expense, state.loaded, monthLabel())
                    if (state.accounts.isNotEmpty()) {
                        Text(
                            "Disponible en cuentas: ${money.format(totalBalance)}",
                            color = MoniColors.mut,
                            style = MaterialTheme.typography.labelMedium.tabular,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
        if (state.pending.isNotEmpty()) {
            item { Entrance(delayMs = 120) { PendingEntry(state.pending.size, onOpenReview) } }
        }
        item {
            Entrance(delayMs = 170) {
                SoftCard(Modifier.fillMaxWidth()) { CategoryChart(slices) }
            }
        }
        if (state.accounts.isNotEmpty()) {
            item {
                Entrance(delayMs = 220) { AccountsSummary(state.accounts, balances) }
            }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Entrance(delayMs = 245) {
                    SoftCard(Modifier.fillMaxWidth()) {
                        UpcomingRemindersCard(upcoming, categoriesById)
                    }
                }
            }
        }
        item {
            Entrance(delayMs = 270) {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Últimos movimientos", color = MoniColors.fg,
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Ver todo", color = MoniColors.iconStroke,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button, onClick = onSeeAll)
                                    .padding(horizontal = 8.dp)
                                    .wrapCenter(),
                            )
                        }
                        if (latest.isEmpty()) {
                            Text("Aún no hay movimientos", color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            latest.forEach { RecentRow(it, categoriesById[it.categoryId]) }
                        }
                    }
                }
            }
        }
    }
}

/** Vertically centers text inside a min-height clickable. */
private fun Modifier.wrapCenter(): Modifier = this.then(Modifier.padding(vertical = 13.dp))

@Composable
private fun CategoryChart(slices: List<CatSlice>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Gasto del mes por categoría", color = MoniColors.fg, style = MaterialTheme.typography.titleSmall)
        if (slices.isEmpty()) {
            Text("Sin gastos este mes", color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium)
        } else {
            val max = slices.maxOf { it.amount }
            slices.forEachIndexed { i, s -> CategoryBarRow(s, (s.amount / max).toFloat(), i) }
        }
    }
}

@Composable
private fun CategoryBarRow(s: CatSlice, fraction: Float, index: Int) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        kotlinx.coroutines.delay(index * 60L)
        progress.animateTo(fraction, moniSpring())
    }
    val label = "${s.name}, ${money.format(s.amount)}, ${s.pct} %"
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = label }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(s.icon)
            Spacer(Modifier.width(10.dp))
            Text(
                s.name, color = MoniColors.fg, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).clearAndSetSemantics { },
            )
            Text(
                money.format(s.amount), color = MoniColors.fg, style = MaterialTheme.typography.labelLarge.tabular,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Text(
                "${s.pct} %", color = MoniColors.mut, style = MaterialTheme.typography.labelMedium.tabular,
                modifier = Modifier.width(44.dp).clearAndSetSemantics { },
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MoniColors.track)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(MoniColors.acc),
            )
        }
    }
}

@Composable
private fun IconTile(icon: Int, tint: Color = MoniColors.iconStroke) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MoniColors.iconTile),
        contentAlignment = Alignment.Center,
    ) { LucideIcon(icon, null, tint = tint, size = 18) }
}

@Composable
private fun AccountsSummary(accounts: List<Account>, balances: Map<String, Double>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Cuentas", color = MoniColors.fg, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
        // Bleed to the screen edges so cards scroll under the gutter.
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 2.dp),
        ) {
            items(accounts, key = { it.id }) { a ->
                val debt = a.type == "credit_card"
                val value = balances[a.id] ?: a.openingBalance
                val caption = if (debt) "Deuda" else "Saldo"
                SoftCard(
                    Modifier
                        .widthIn(min = 140.dp, max = 220.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "${a.name}, $caption ${money.format(value)}" },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(a.name, color = MoniColors.mut, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            money.format(value),
                            color = if (debt && value > 0) MoniColors.exp else MoniColors.fg,
                            style = MaterialTheme.typography.titleMedium.tabular,
                            maxLines = 1,
                        )
                        if (debt) Text("Deuda", color = MoniColors.mut, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRow(t: Transaction, category: Category?) {
    val income = t.isIncome
    val name = category?.name ?: if (income) "Ingreso" else "Gasto"
    val amount = (if (income) "+" else "−") + money.format(t.amount)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name, ${formatDate(t.occurredAt)}, ${if (income) "ingreso" else "gasto"} ${money.format(t.amount)}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(categoryIcon(category, income))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = MoniColors.fg, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatDate(t.occurredAt), color = MoniColors.mut, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            amount,
            color = if (income) MoniColors.inc else MoniColors.exp,
            style = MaterialTheme.typography.labelLarge.tabular,
        )
    }
}

private fun frequencyLabel(frequency: String?): String = when (frequency) {
    "weekly" -> "Semanal"
    "monthly" -> "Mensual"
    "yearly" -> "Anual"
    else -> ""
}

@Composable
private fun UpcomingRemindersCard(reminders: List<Reminder>, categoriesById: Map<String, Category>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Próximos pagos y cobros",
            color = MoniColors.fg,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        reminders.forEach { r -> UpcomingReminderRow(r, categoriesById[r.categoryId]) }
    }
}

@Composable
private fun UpcomingReminderRow(r: Reminder, category: Category?) {
    val isIncome = r.kind == "receivable"
    val freq = frequencyLabel(r.frequency)
    val date = formatDate(r.dueDate)
    val subtitle = listOf(freq, date).filter { it.isNotEmpty() }.joinToString(" · ")
    val amountText = r.amount?.let { (if (isIncome) "+" else "−") + money.format(it) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(
                    r.title,
                    subtitle.takeIf { it.isNotEmpty() },
                    r.amount?.let { (if (isIncome) "ingreso " else "gasto ") + money.format(it) },
                ).joinToString(", ")
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(categoryIcon(category, isIncome))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, color = MoniColors.fg, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MoniColors.mut, style = MaterialTheme.typography.labelSmall.tabular)
        }
        if (amountText != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                amountText,
                color = if (isIncome) MoniColors.inc else MoniColors.exp,
                style = MaterialTheme.typography.labelLarge.tabular,
            )
        }
    }
}
