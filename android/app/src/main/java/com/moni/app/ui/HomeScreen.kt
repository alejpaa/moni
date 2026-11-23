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
    onAdd: (String, Double, String, String?, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onSignOut: () -> Unit,
    onClearError: () -> Unit,
) {
    val summary = state.transactions.summary()
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
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
                onSubmit = { type, amount, category, note ->
                    onAdd(type, amount, category, note) { showSheet = false }
                },
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
                LucideIcon(categoryIcon(t.category, t.isIncome), null, tint = MoniColors.iconStroke)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    t.category,
                    color = MoniColors.fg,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatDate(t.occurredAt) + (t.note?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
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

@Composable
private fun AddSheet(
    busy: Boolean,
    error: String?,
    onSubmit: (String, Double, String, String?) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(0) } // 0 = Gasto, 1 = Ingreso
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val value = amount.replace(',', '.').toDoubleOrNull()
    val amountError = if (submitted && (value == null || value <= 0)) "Introduce un monto mayor que 0" else null
    val categoryError = if (submitted && category.isBlank()) "Escribe una categoría (p. ej. Comida)" else null

    fun submit() {
        submitted = true
        if (busy || value == null || value <= 0 || category.isBlank()) return
        onSubmit(if (type == 1) "income" else "expense", value, category.trim(), note.trim().ifBlank { null })
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
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Categoría") },
            singleLine = true,
            enabled = !busy,
            isError = categoryError != null,
            shape = FieldShape,
            colors = moniFieldColors(),
            leadingIcon = { LucideIcon(categoryIcon(category, type == 1), null, tint = MoniColors.iconStroke) },
            supportingText = categoryError?.let { { FieldMessage(it, true) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
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
