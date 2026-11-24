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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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

/** Remembers which ids existed at first load so later arrivals can be highlighted without replaying on tab switches. */
class RowTracker {
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

    /** Registers paginated older rows without highlighting them as newly added. */
    fun markBaseline(ids: List<String>) {
        baseline = (baseline ?: emptySet()) + ids
        animated += ids
    }

    fun reset() {
        baseline = null
        newIds.clear()
        animated.clear()
    }
}

/** "Movimientos" tab: filters, pending-review entry and the paginated transaction list. */
@Composable
fun TransactionsScreen(
    state: UiState,
    contentPadding: PaddingValues,
    tracker: RowTracker,
    onDelete: (String) -> Unit,
    onOpenReview: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onRetrySession: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val categoriesById = remember(state.allCategories) { state.allCategories.associateBy { it.id } }
    val retry = if (state.sessionRefreshFailed) onRetrySession else onRetry

    val visible = remember(state.transactions, filter) {
        when (filter) {
            Filter.All -> state.transactions
            Filter.Income -> state.transactions.filter { it.isIncome }
            Filter.Expense -> state.transactions.filterNot { it.isIncome }
        }
    }

    Column(
        Modifier
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
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
            visible = !state.isViewer && state.pending.isNotEmpty(),
            enter = expandVertically(moniSpring()) + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) { PendingEntry(state.pending.size, onOpenReview) }

        if (state.loadFailed && !state.loaded) {
            ErrorRetryCard(
                message = state.error ?: "No se pudieron cargar tus movimientos",
                onRetry = retry,
            )
            return@Column
        }

        state.error?.let { AlertBanner(it, actionLabel = "Reintentar", onAction = retry) }

        val cardShape = RoundedCornerShape(20.dp)
        Entrance(Modifier.weight(1f), delayMs = 140) {
            Box(
                Modifier
                    .fillMaxSize()
                    .shadow(8.dp, cardShape, ambientColor = Color(0x331F1E22), spotColor = Color(0x331F1E22))
                    .clip(cardShape)
                    .background(MoniColors.card),
            ) {
                if (visible.isEmpty()) {
                    EmptyState(
                        loaded = state.loaded,
                        filtered = state.transactions.isNotEmpty(),
                        hasMore = state.hasMoreTransactions,
                        loadingMore = state.loadingMoreTransactions,
                        onLoadMore = onLoadMore,
                        bottomPadding = contentPadding.calculateBottomPadding(),
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 8.dp),
                    ) {
                        itemsIndexed(visible, key = { _, t -> t.id }) { index, t ->
                            TransactionRow(
                                t = t,
                                category = categoriesById[t.categoryId],
                                index = index,
                                last = index == visible.lastIndex && !state.hasMoreTransactions,
                                tracker = tracker,
                                canDelete = !state.isViewer,
                                deleting = t.id in state.deletingIds,
                                onRequestDelete = { pendingDeleteId = t.id },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        if (state.hasMoreTransactions) {
                            item(key = "load-more") {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LoadMoreButton(loading = state.loadingMoreTransactions, onClick = onLoadMore)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val deleteTarget = pendingDeleteId?.let { id -> state.transactions.find { it.id == id } }
    if (deleteTarget != null) {
        val isInstallment = deleteTarget.source == "installment"
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            containerColor = MoniColors.card,
            title = {
                Text(
                    if (isInstallment) "¿Omitir cuota?" else "¿Eliminar movimiento?",
                    color = MoniColors.fg,
                )
            },
            text = {
                Text(
                    if (isInstallment) {
                        "Esta cuota se marcará como ignorada para que no vuelva a crearse automáticamente."
                    } else {
                        "Esta acción no se puede deshacer."
                    },
                    color = MoniColors.mut,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = deleteTarget.id !in state.deletingIds,
                    onClick = {
                        val id = deleteTarget.id
                        pendingDeleteId = null
                        onDelete(id)
                    },
                ) {
                    Text(if (isInstallment) "Ignorar cuota" else "Eliminar", color = MoniColors.exp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancelar", color = MoniColors.fg) }
            },
        )
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
private fun LoadMoreButton(loading: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(16.dp), color = MoniColors.acc, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Cargando…", color = MoniColors.mut)
        } else {
            Text("Cargar más", color = MoniColors.iconStroke)
        }
    }
}

@Composable
private fun EmptyState(
    loaded: Boolean,
    filtered: Boolean,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    Column(
        Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!loaded) return@Column
        if (filtered) {
            IconTile(LucideR.drawable.lucide_ic_wallet, size = 56.dp, radius = 16.dp, iconSize = 24)
        } else {
            MoniMascot(112.dp)
        }
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
        if (hasMore) {
            Spacer(Modifier.height(12.dp))
            LoadMoreButton(loading = loadingMore, onClick = onLoadMore)
        }
    }
}

@Composable
private fun TransactionRow(
    t: Transaction,
    category: Category?,
    index: Int,
    last: Boolean,
    tracker: RowTracker,
    canDelete: Boolean,
    deleting: Boolean,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val animScale = remember(context) { systemAnimatorScale(context) }
    val isNew = t.id in tracker.newIds
    val skipEntrance = animScale <= 0f || t.id in tracker.animated
    val progress = remember { Animatable(if (skipEntrance) 1f else 0f) }
    val offsetPx = with(LocalDensity.current) { 30.dp.toPx() }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            if (!isNew) {
                val d = scaledDelayMs(70L * index.coerceAtMost(10), animScale)
                if (d > 0) delay(d)
            }
            progress.animateTo(1f, moniSpring())
        }
        tracker.animated += t.id
    }

    var faded by remember { mutableStateOf(false) }
    LaunchedEffect(isNew) {
        if (isNew) {
            val d = scaledDelayMs(600, animScale)
            if (d > 0) delay(d)
            faded = true
        }
    }
    val tint = animateColorAsState(
        targetValue = if (isNew && !faded) MoniColors.newTint else Color.Transparent,
        animationSpec = if (faded && animScale > 0f) tween(1600) else snap(),
        finishedListener = {
            if (faded) tracker.newIds.remove(t.id)
        },
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
                .drawBehind {
                    val c = tint.value
                    if (c.alpha > 0f) drawRect(c)
                }
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = if (canDelete) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(categoryIcon(category, t.isIncome))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        category?.name ?: t.merchant?.takeIf { it.isNotBlank() } ?: "Sin categoría",
                        color = MoniColors.fg,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (t.reminderId != null) {
                        LucideIcon(
                            LucideR.drawable.lucide_ic_repeat,
                            contentDescription = "Recurrente",
                            tint = MoniColors.iconStroke,
                            size = 13,
                        )
                    }
                }
                Text(
                    formatDate(t.occurredAt) + (listOfNotNull(t.merchant.takeIf { category != null }, t.note).firstOrNull { !it.isNullOrBlank() }?.let { " · $it" } ?: ""),
                    color = MoniColors.mut,
                    style = MaterialTheme.typography.bodySmall.tabular,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            val reveal = rememberRevealState()
            Text(
                formatSignedMoney(t.amount, t.isIncome, reveal.effectiveHidden),
                color = if (t.isIncome) MoniColors.inc else MoniColors.exp,
                style = MaterialTheme.typography.titleSmall.tabular.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                modifier = Modifier.revealOnTap(reveal),
            )
            if (canDelete) {
                IconButton(onClick = onRequestDelete, enabled = !deleting) {
                    LucideIcon(LucideR.drawable.lucide_ic_trash_2, "Eliminar movimiento", tint = MoniColors.mut)
                }
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(MoniColors.divider))
    }
}
