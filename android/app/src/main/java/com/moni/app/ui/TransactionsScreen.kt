package com.moni.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Category
import com.moni.app.data.Transaction
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular
import kotlinx.coroutines.delay

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

/** "Movimientos" tab: filters, pending-review entry and the transaction list. */
@Composable
fun TransactionsScreen(
    state: UiState,
    contentPadding: PaddingValues,
    onDelete: (String) -> Unit,
    onOpenReview: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val tracker = remember { RowTracker() }
    remember(state.transactions, state.loaded) { tracker.update(state.transactions.map { it.id }, state.loaded) }

    val visible = when (filter) {
        Filter.All -> state.transactions
        Filter.Income -> state.transactions.filter { it.isIncome }
        Filter.Expense -> state.transactions.filterNot { it.isIncome }
    }

    Column(
        Modifier.padding(contentPadding).padding(horizontal = 16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Entrance { ScreenHeader("Movimientos") }
        Entrance(delayMs = 70) {
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
        ) { PendingEntry(state.pending.size, onOpenReview) }
        state.error?.let { AlertBanner(it) }

        val cardShape = RoundedCornerShape(20.dp)
        Entrance(Modifier.weight(1f), delayMs = 140) {
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
