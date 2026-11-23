package com.moni.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.moni.app.data.Account
import com.moni.app.data.Category
import com.moni.app.data.Transaction
import com.moni.app.data.summary
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val esLocale = Locale.forLanguageTag("es")
private val money = NumberFormat.getNumberInstance(esLocale).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
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

private enum class Filter(val label: String) { All("Todo"), Income("Ingresos"), Expense("Gastos") }

/** Remembers which ids existed at first load so later arrivals can be highlighted. */
private class RowTracker {
    var baseline: Set<String>? = null
    val newIds = mutableSetOf<String>()
    val animated = mutableSetOf<String>()

    fun update(ids: List<String>, loaded: Boolean) {
        if (!loaded) return
        val b = baseline
        if (b == null) baseline = ids.toSet()
        else {
            newIds += ids.filter { it !in b }
            baseline = b + ids
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onAdd: (String, Double, String, String?, String?, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onConfirm: (String, String?, String?) -> Unit,
    onIgnore: (String) -> Unit,
    onSignOut: () -> Unit,
    onClearError: () -> Unit,
) {
    val summary = state.transactions.summary()
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    // Closes the review sheet once the last pending item is resolved.
    LaunchedEffect(state.pending.isEmpty()) { if (state.pending.isEmpty()) showReview = false }
    val tracker = remember { RowTracker() }
    remember(state.transactions, state.loaded) { tracker.update(state.transactions.map { it.id }, state.loaded) }

    val visible = when (filter) {
        Filter.All -> state.transactions
        Filter.Income -> state.transactions.filter { it.isIncome }
        Filter.Expense -> state.transactions.filterNot { it.isIncome }
    }

    Scaffold(
        containerColor = MoniColors.bg,
        bottomBar = {
            Box(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Entrance(delayMs = 280) {
                    PrimaryButton(
                        "Añadir movimiento",
                        onClick = { onClearError(); showSheet = true },
                        icon = LucideR.drawable.lucide_ic_plus,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Entrance {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("MONI", color = MoniColors.mut, style = MaterialTheme.typography.labelSmall)
                        Text("Tus finanzas", color = MoniColors.fg, style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onSignOut) {
                        LucideIcon(LucideR.drawable.lucide_ic_log_out, "Cerrar sesión", tint = MoniColors.mut, size = 20)
                    }
                }
            }
            Entrance(delayMs = 70, initialScale = 0.92f) {
                HeroCard(summary.balance, summary.income, summary.expense, state.loaded)
            }
            Entrance(delayMs = 140) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Filter.entries.forEach { f ->
                        FilterPill(f.label, selected = filter == f) { filter = f }
                    }
                }
            }
            AnimatedVisibility(
                visible = state.pending.isNotEmpty(),
                enter = expandVertically(moniSpring()) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) { PendingEntry(state.pending.size) { onClearError(); showReview = true } }
            state.error?.let { AlertBanner(it) }

            val cardShape = RoundedCornerShape(20.dp)
            Entrance(Modifier.weight(1f), delayMs = 210) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 4.dp)
                        .shadow(8.dp, cardShape, ambientColor = Color(0x331F1E22), spotColor = Color(0x331F1E22))
                        .clip(cardShape)
                        .background(MoniColors.card),
                ) {
                    if (visible.isEmpty()) {
                        EmptyState(state.loaded, filtered = state.transactions.isNotEmpty())
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            itemsIndexed(visible, key = { _, t -> t.id }) { index, t ->
                                TransactionRow(
                                    t = t,
                                    category = categoriesById[t.categoryId],
                                    index = index,
                                    last = index == visible.lastIndex,
                                    tracker = tracker,
                                    onDelete = onDelete,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!state.busy) { showSheet = false; onClearError() } },
            sheetState = sheetState,
            containerColor = MoniColors.card,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            AddSheet(
                busy = state.busy,
                error = state.error,
                categories = state.categories,
                accounts = state.accounts,
                onSubmit = { type, amount, categoryId, accountId, note ->
                    onAdd(type, amount, categoryId, accountId, note) { showSheet = false }
                },
            )
        }
    }

    if (showReview) {
        ModalBottomSheet(
            onDismissRequest = { showReview = false; onClearError() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MoniColors.card,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            ReviewSheet(state.pending, state.categories, state.accounts, state.error, onConfirm, onIgnore)
        }
    }
}

@Composable
private fun PendingEntry(count: Int, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val label = if (count == 1) "1 movimiento por revisar" else "$count movimientos por revisar"
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pressScale(source)
            .clip(RoundedCornerShape(16.dp))
            .background(MoniColors.iconTile)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Por revisar: $label" }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(LucideR.drawable.lucide_ic_inbox, null, tint = MoniColors.iconStroke, size = 20)
        Spacer(Modifier.width(12.dp))
        Text("Por revisar", color = MoniColors.fg, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            count.toString(),
            color = MoniColors.accFg,
            style = MaterialTheme.typography.labelLarge.tabular,
            modifier = Modifier.clip(CircleShape).background(MoniColors.acc).padding(horizontal = 10.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        LucideIcon(LucideR.drawable.lucide_ic_chevron_right, null, tint = MoniColors.mut)
    }
}

/** Selectable chip (icon + name), >= 48dp tall, color-animated like the filter pills. */
@Composable
private fun ChoiceChip(label: String, icon: Int?, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) MoniColors.acc else MoniColors.iconTile, label = "choiceBg")
    val fg by animateColorAsState(if (selected) MoniColors.accFg else MoniColors.fg, label = "choiceFg")
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .pressScale(source)
            .selectable(selected = selected, enabled = enabled, interactionSource = source, indication = null, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.clip(CircleShape).background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                LucideIcon(icon, null, tint = fg, size = 16)
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun ReviewSheet(
    pending: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    error: String?,
    onConfirm: (String, String?, String?) -> Unit,
    onIgnore: (String) -> Unit,
) {
    Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Text("Por revisar", color = MoniColors.fg, style = MaterialTheme.typography.titleLarge)
        Text(
            "Confirma o ignora los movimientos capturados automáticamente.",
            color = MoniColors.mut,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        AlertBanner(error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
            itemsIndexed(pending, key = { _, t -> t.id }) { _, t ->
                ReviewItem(t, categories, accounts, onConfirm, onIgnore, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ReviewItem(
    t: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    onConfirm: (String, String?, String?) -> Unit,
    onIgnore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryId by rememberSaveable(t.id) { mutableStateOf(t.categoryId) }
    var accountId by rememberSaveable(t.id) { mutableStateOf(t.accountId) }
    var working by rememberSaveable(t.id) { mutableStateOf(false) }
    val kindCategories = categories.filter { it.kind == t.type }
    val title = t.merchant?.takeIf { it.isNotBlank() } ?: t.note?.takeIf { it.isNotBlank() }
        ?: if (t.isIncome) "Ingreso" else "Gasto"
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(MoniColors.bg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = MoniColors.fg, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDate(t.occurredAt), color = MoniColors.mut, style = MaterialTheme.typography.bodySmall.tabular)
            }
            Text(
                (if (t.isIncome) "+" else "−") + money.format(t.amount),
                color = if (t.isIncome) MoniColors.inc else MoniColors.exp,
                style = MaterialTheme.typography.titleSmall.tabular.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Text("Categoría", color = MoniColors.mut, style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            kindCategories.forEach { c ->
                ChoiceChip(c.name, categoryIcon(c, t.isIncome), c.id == categoryId, !working) { categoryId = c.id }
            }
        }
        if (accounts.isNotEmpty()) {
            Text("Cuenta", color = MoniColors.mut, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                accounts.forEach { a ->
                    ChoiceChip(a.name, null, a.id == accountId, !working) { accountId = a.id }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !working, role = Role.Button) { working = true; onIgnore(t.id) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Ignorar", color = MoniColors.mut, style = MaterialTheme.typography.labelLarge) }
            PrimaryButton(
                "Confirmar",
                onClick = { working = true; onConfirm(t.id, categoryId, accountId) },
                enabled = !working,
                icon = LucideR.drawable.lucide_ic_check,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroCard(balance: Double, income: Double, expense: Double, loaded: Boolean) {
    // Counts up over ~900ms on first load, 500ms on later changes.
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

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) MoniColors.acc else MoniColors.card, label = "chipBg")
    val fg by animateColorAsState(if (selected) MoniColors.accFg else MoniColors.fg, label = "chipFg")
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .pressScale(source)
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .shadow(1.dp, CircleShape, ambientColor = Color(0x0F000000), spotColor = Color(0x0F000000))
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EmptyState(loaded: Boolean, filtered: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!loaded) return@Column
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MoniColors.iconTile),
            contentAlignment = Alignment.Center,
        ) { LucideIcon(LucideR.drawable.lucide_ic_wallet, null, tint = MoniColors.iconStroke, size = 24) }
        Spacer(Modifier.height(12.dp))
        Text(
            if (filtered) "Sin movimientos aquí" else "Aún no hay movimientos",
            color = MoniColors.fg,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (filtered) "Prueba con otro filtro." else "Añade tu primer ingreso o gasto para empezar.",
            color = MoniColors.mut,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TransactionRow(
    t: Transaction,
    category: Category?,
    index: Int,
    last: Boolean,
    tracker: RowTracker,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNew = t.id in tracker.newIds
    val skipEntrance = t.id in tracker.animated
    val progress = remember { Animatable(if (skipEntrance) 1f else 0f) }
    val offsetPx = with(LocalDensity.current) { 30.dp.toPx() }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            // Initial rows stagger 70ms apart; a new row enters immediately.
            if (!isNew) delay(70L * index.coerceAtMost(10))
            progress.animateTo(1f, moniSpring())
        }
        tracker.animated += t.id
    }

    var faded by remember { mutableStateOf(false) }
    LaunchedEffect(isNew) { if (isNew) { delay(600); faded = true } }
    val tint by animateColorAsState(
        targetValue = if (isNew && !faded) MoniColors.newTint else Color.Transparent,
        animationSpec = if (faded) tween(1600) else snap(),
        label = "newTint",
    )

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val p = progress.value
                    alpha = p.coerceIn(0f, 1f)
                    if (isNew) {
                        translationX = -offsetPx * (1f - p)
                        scaleX = 0.9f + 0.1f * p
                        scaleY = 0.9f + 0.1f * p
                    } else {
                        translationY = 18.dp.toPx() * (1f - p)
                    }
                }
                .background(tint)
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MoniColors.iconTile),
                contentAlignment = Alignment.Center,
            ) {
                LucideIcon(categoryIcon(category, t.isIncome), null, tint = MoniColors.iconStroke)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    category?.name ?: t.merchant?.takeIf { it.isNotBlank() } ?: "Sin categoría",
                    color = MoniColors.fg,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatDate(t.occurredAt) + (listOfNotNull(t.merchant.takeIf { category != null }, t.note).firstOrNull { !it.isNullOrBlank() }?.let { " · $it" } ?: ""),
                    color = MoniColors.mut,
                    style = MaterialTheme.typography.bodySmall.tabular,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (t.isIncome) "+" else "−") + money.format(t.amount),
                color = if (t.isIncome) MoniColors.inc else MoniColors.exp,
                style = MaterialTheme.typography.titleSmall.tabular.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            IconButton(onClick = { onDelete(t.id) }) {
                LucideIcon(LucideR.drawable.lucide_ic_trash_2, "Eliminar movimiento", tint = MoniColors.mut)
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(MoniColors.divider))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSheet(
    busy: Boolean,
    error: String?,
    categories: List<Category>,
    accounts: List<Account>,
    onSubmit: (String, Double, String, String?, String?) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(0) } // 0 = Gasto, 1 = Ingreso
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var accountPick by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val kind = if (type == 1) "income" else "expense"
    val shown = categories.filter { it.kind == kind }
    // Switching Gasto/Ingreso drops a category of the other kind.
    val category = shown.firstOrNull { it.id == categoryId }
    // Default: the first non-archived account (Efectivo is seeded per workspace).
    val accountId = accountPick ?: accounts.firstOrNull { !it.archived }?.id

    val value = amount.replace(',', '.').toDoubleOrNull()
    val amountError = if (submitted && (value == null || value <= 0)) "Introduce un monto mayor que 0" else null
    val categoryError = if (submitted && category == null) "Elige una categoría" else null

    fun submit() {
        submitted = true
        if (busy || value == null || value <= 0 || category == null) return
        onSubmit(kind, value, category.id, accountId, note.trim().ifBlank { null })
    }

    Column(
        Modifier
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Nuevo movimiento", color = MoniColors.fg, style = MaterialTheme.typography.titleLarge)
        SegmentedControl(listOf("Gasto", "Ingreso"), type, { if (!busy) type = it })
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Monto") },
            singleLine = true,
            enabled = !busy,
            isError = amountError != null,
            shape = FieldShape,
            colors = moniFieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge.tabular,
            supportingText = amountError?.let { { FieldMessage(it, true) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Categoría", color = MoniColors.mut, style = MaterialTheme.typography.labelMedium)
        if (shown.isEmpty()) {
            Text("Cargando categorías…", color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            shown.forEach { c ->
                ChoiceChip(c.name, categoryIcon(c, type == 1), c.id == category?.id, !busy) { categoryId = c.id }
            }
        }
        categoryError?.let { FieldMessage(it, true) }
        if (accounts.isNotEmpty()) {
            Text("Cuenta", color = MoniColors.mut, style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                accounts.forEach { a ->
                    ChoiceChip(a.name, null, a.id == accountId, !busy) { accountPick = a.id }
                }
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Nota (opcional)") },
            singleLine = true,
            enabled = !busy,
            shape = FieldShape,
            colors = moniFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        AlertBanner(error)
        PrimaryButton(
            text = if (busy) "Guardando…" else "Guardar",
            onClick = { submit() },
            enabled = !busy,
            loading = busy,
        )
    }
}
