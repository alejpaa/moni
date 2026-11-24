package com.moni.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Account
import com.moni.app.data.AccountBalance
import com.moni.app.data.AccountInput
import com.moni.app.data.computeBalanceTotals
import com.moni.app.data.round2
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular
import kotlin.math.roundToInt

internal val accountTypes = listOf("savings", "debit", "yape", "cash", "credit_card", "wallet", "investment", "other")

internal fun accountTypeLabel(type: String) = when (type) {
    "savings" -> "Ahorros"
    "debit" -> "Débito"
    "yape" -> "Yape"
    "cash" -> "Efectivo"
    "credit_card" -> "Tarjeta de crédito"
    "wallet" -> "Billetera"
    "investment" -> "Inversión"
    else -> "Otra"
}

internal fun accountTypeIcon(type: String) = when (type) {
    "savings" -> LucideR.drawable.lucide_ic_landmark
    "debit", "credit_card" -> LucideR.drawable.lucide_ic_credit_card
    "yape" -> LucideR.drawable.lucide_ic_smartphone
    "cash" -> LucideR.drawable.lucide_ic_banknote
    "wallet" -> LucideR.drawable.lucide_ic_wallet
    "investment" -> LucideR.drawable.lucide_ic_trending_up
    else -> LucideR.drawable.lucide_ic_circle_dollar_sign
}

/**
 * "Cuentas" tab. Reads exclusively from the shared [state] store; switching tabs causes zero
 * network calls. Only account mutations update the accounts/balances slices.
 */
@Composable
fun AccountsScreen(
    state: UiState,
    contentPadding: PaddingValues,
    onClearError: () -> Unit,
    onSave: (id: String?, input: AccountInput, onDone: () -> Unit) -> Unit,
    onSetArchived: (id: String, archived: Boolean, fromSheet: Boolean, onDone: () -> Unit) -> Unit,
    onRetry: () -> Unit,
    onRetrySession: () -> Unit,
) {
    var formOpen by rememberSaveable { mutableStateOf(false) }
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }

    val active = remember(state.allAccounts) { state.allAccounts.filter { !it.archived } }
    val archived = remember(state.allAccounts) { state.allAccounts.filter { it.archived } }
    val ready = state.accountsLoaded && state.balancesLoaded
    val totals = remember(state.allAccounts, state.balances) { computeBalanceTotals(state.allAccounts, state.balances) }
    val retry = if (state.sessionRefreshFailed) onRetrySession else onRetry

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp + contentPadding.calculateStartPadding(LayoutDirection.Ltr),
            end = 16.dp + contentPadding.calculateEndPadding(LayoutDirection.Ltr),
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item(key = "header") { Entrance { ScreenHeader("Cuentas") } }
        if (state.loadFailed && !ready) {
            item(key = "main-error") {
                ErrorRetryCard(
                    message = state.error ?: "No se pudieron cargar tus cuentas",
                    onRetry = retry,
                )
            }
            return@LazyColumn
        }
        item(key = "hero") {
            Entrance(delayMs = 60) {
                val availReveal = rememberRevealState()
                val debtReveal = rememberRevealState()
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MoniColors.hero).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Disponible", color = MoniColors.heroFg, style = MaterialTheme.typography.labelMedium)
                    if (!ready) {
                        SkeletonBox(
                            Modifier.width(156.dp).height(32.dp),
                            color = MoniColors.heroFg.copy(alpha = 0.16f),
                            radius = 8.dp,
                        )
                    } else {
                        Text(
                            formatSoles(totals.disponible, availReveal.effectiveHidden),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium.tabular,
                            modifier = Modifier.revealOnTap(availReveal, label = "Disponible"),
                        )
                        if (totals.hasCards) {
                            Spacer(Modifier.height(6.dp))
                            Text("Deuda de tarjetas", color = MoniColors.heroFg, style = MaterialTheme.typography.labelMedium)
                            Text(
                                formatSoles(totals.cardDebt, debtReveal.effectiveHidden),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.tabular,
                                modifier = Modifier.revealOnTap(debtReveal, label = "Deuda de tarjetas"),
                            )
                        }
                    }
                }
            }
        }
        if (!state.isViewer) {
            item(key = "new-button") {
                Entrance(delayMs = 100) {
                    PrimaryButton(
                        "Nueva cuenta",
                        onClick = { onClearError(); editingAccountId = null; formOpen = true },
                        icon = LucideR.drawable.lucide_ic_plus,
                    )
                }
            }
        }
        if (state.error != null && !formOpen) {
            item(key = "error") { AlertBanner(state.error, actionLabel = "Reintentar", onAction = retry) }
        }
        if (!ready) {
            items(2, key = { "account-skel-$it" }) { AccountSkeletonCard() }
        } else if (active.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Aún no tienes cuentas. Crea la primera para empezar.",
                    color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(active, key = { it.id }) { a ->
                val bal = state.balances[a.id]
                AccountCard(
                    a = a,
                    balance = bal?.effectiveBalance ?: a.openingBalance,
                    accountBalance = bal,
                    onClick = if (state.isViewer) null else {
                        { onClearError(); editingAccountId = a.id; formOpen = true }
                    },
                )
            }
        }
        if (ready && archived.isNotEmpty()) {
            item(key = "archived-header") {
                val rotation by animateFloatAsState(if (showArchived) 180f else 0f, moniSpring(), label = "chev")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { showArchived = !showArchived }
                        .heightMin().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Archivadas (${archived.size})", color = MoniColors.mut, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    LucideIcon(LucideR.drawable.lucide_ic_chevron_down, null, tint = MoniColors.mut, modifier = Modifier.rotateBy(rotation))
                }
            }
            item(key = "archived-list") {
                AnimatedVisibility(
                    visible = showArchived,
                    enter = expandVertically(moniSpring()) + fadeIn(),
                    exit = shrinkVertically(moniSpring()) + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        archived.forEach { a ->
                            ArchivedRow(
                                a = a,
                                canRestore = !state.isViewer,
                                enabled = !state.busy,
                                onRestore = { onSetArchived(a.id, false, false) {} },
                            )
                        }
                    }
                }
            }
        }
    }

    if (formOpen && !state.isViewer) {
        val account = editingAccountId?.let { id -> state.allAccounts.find { it.id == id } }
        AccountFormSheet(
            account = account,
            busy = state.busy,
            error = state.sheetError,
            onSave = { input -> onSave(account?.id, input) { formOpen = false; editingAccountId = null } },
            onArchive = { account?.let { onSetArchived(it.id, true, true) { formOpen = false; editingAccountId = null } } },
            onDismiss = { formOpen = false; editingAccountId = null; onClearError() },
        )
    }
}

@Composable
private fun AccountSkeletonCard() {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(shape)
            .background(MoniColors.card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(Modifier.size(44.dp), radius = 14.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBox(Modifier.width(110.dp).height(15.dp), radius = 6.dp)
            SkeletonBox(Modifier.width(72.dp).height(12.dp), radius = 6.dp)
        }
        SkeletonBox(Modifier.width(74.dp).height(16.dp), radius = 6.dp)
    }
}

private fun Modifier.heightMin() = this.then(Modifier.defaultMinSize(minHeight = 48.dp))

private fun Modifier.rotateBy(deg: Float) = this.then(Modifier.graphicsLayer { rotationZ = deg })

@Composable
private fun AccountCard(
    a: Account,
    balance: Double,
    accountBalance: AccountBalance?,
    onClick: (() -> Unit)?,
) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    val card = a.type == "credit_card"
    val baseModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.pressScale(source) else it }
        .shadow(2.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
        .clip(shape)
        .background(MoniColors.card)
        .let {
            if (onClick != null) {
                it.clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            } else {
                it
            }
        }
        .padding(16.dp)
        .animateContentSize(moniSpring())

    Column(baseModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(accountTypeIcon(a.type), size = 44.dp, radius = 14.dp, iconSize = 22)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.name, color = MoniColors.fg, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                val detail = listOfNotNull(
                    a.institution?.takeIf { it.isNotBlank() },
                    a.last4?.takeIf { it.isNotBlank() }?.let { "••••$it" },
                ).joinToString(" · ")
                Text(
                    listOf(accountTypeLabel(a.type), detail).filter { it.isNotBlank() }.joinToString(" · "),
                    color = MoniColors.mut, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            val balanceReveal = rememberRevealState()
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.revealOnTap(balanceReveal, label = a.name),
            ) {
                if (card) Text("Deuda", color = MoniColors.mut, style = MaterialTheme.typography.labelSmall)
                Text(
                    formatSoles(balance, balanceReveal.effectiveHidden),
                    color = if (card) MoniColors.exp else if (round2(balance) < 0 && !balanceReveal.effectiveHidden) MoniColors.exp else MoniColors.fg,
                    style = MaterialTheme.typography.titleSmall.tabular,
                )
            }
        }
        if (card) CardUsage(a, balance, accountBalance)
    }
}

@Composable
private fun CardUsage(a: Account, debt: Double, accountBalance: AccountBalance?) {
    val limit = a.creditLimit
    if (limit != null && limit > 0) {
        val pendingInstallments = accountBalance?.pendingInstallments ?: 0.0
        val available = accountBalance?.availableCredit ?: (limit - debt - pendingInstallments)
        val used = (limit - available).coerceAtLeast(0.0)
        val fraction = (used / limit).toFloat().coerceIn(0f, 1f)
        val pct = ((used / limit) * 100.0).roundToInt().coerceIn(0, 100)
        val animated = animateFloatAsState(fraction, moniSpring(), label = "usage")
        val barColor = if (fraction >= 0.9f) MoniColors.exp else MoniColors.acc
        val availReveal = rememberRevealState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Disponible ($pct% usado)",
                color = MoniColors.mut,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatSoles(available, availReveal.effectiveHidden),
                color = MoniColors.fg,
                style = MaterialTheme.typography.bodySmall.tabular,
                modifier = Modifier.revealOnTap(availReveal, label = "Crédito disponible"),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .drawBehind {
                    val r = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(MoniColors.track, cornerRadius = r)
                    val w = size.width * animated.value.coerceIn(0f, 1f)
                    if (w > 0f) {
                        drawRoundRect(barColor, size = Size(w, size.height), cornerRadius = r)
                    }
                },
        )
        if (round2(pendingInstallments) > 0) {
            val pendingReveal = rememberRevealState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cuotas pendientes", color = MoniColors.mut, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(
                    formatSoles(pendingInstallments, pendingReveal.effectiveHidden),
                    color = MoniColors.mut,
                    style = MaterialTheme.typography.bodySmall.tabular,
                    modifier = Modifier.revealOnTap(pendingReveal, label = "Cuotas pendientes"),
                )
            }
        }
    }
    if (a.closingDay != null && a.dueDay != null) {
        Text(
            "Cierra el día ${a.closingDay} · Paga el día ${a.dueDay}",
            color = MoniColors.mut, style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ArchivedRow(a: Account, canRestore: Boolean, enabled: Boolean, onRestore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MoniColors.card).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(accountTypeIcon(a.type), null, tint = MoniColors.mut)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.name, color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(accountTypeLabel(a.type), color = MoniColors.mut, style = MaterialTheme.typography.bodySmall)
        }
        if (canRestore) {
            TextButton(onClick = onRestore, enabled = enabled, modifier = Modifier.height(48.dp)) {
                LucideIcon(LucideR.drawable.lucide_ic_archive_restore, null, tint = MoniColors.iconStroke, size = 16)
                Spacer(Modifier.width(6.dp))
                Text("Restaurar", color = MoniColors.iconStroke)
            }
        }
    }
}
