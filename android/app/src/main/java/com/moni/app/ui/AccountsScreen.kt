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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Account
import com.moni.app.data.Transaction
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular

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

private fun soles(v: Double) = (if (v < 0) "-S/ " else "S/ ") + money.format(kotlin.math.abs(v))

/** Which account the form sheet is showing: none, a new one, or an existing one. */
private sealed interface FormTarget {
    data object New : FormTarget
    data class Edit(val account: Account) : FormTarget
}

/**
 * "Cuentas" tab. [transactions] are the confirmed ones; balances are reloaded when they change.
 * [onAccountsChanged] fires after every create/edit/archive/restore so the add sheet sees the new list.
 */
@Composable
fun AccountsScreen(
    transactions: List<Transaction>,
    contentPadding: PaddingValues,
    onAccountsChanged: () -> Unit,
) {
    val vm: AccountsViewModel = viewModel()
    val state by vm.state.collectAsState()
    LaunchedEffect(transactions) { vm.reload() }

    var form by remember { mutableStateOf<FormTarget?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }

    val active = state.accounts.filter { !it.archived }
    val archived = state.accounts.filter { it.archived }
    val available = active.filter { it.type != "credit_card" }.sumOf { state.balances[it.id] ?: 0.0 }
    val cardDebt = active.filter { it.type == "credit_card" }.sumOf { state.balances[it.id] ?: 0.0 }
    val hasCards = active.any { it.type == "credit_card" }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp + contentPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            end = 16.dp + contentPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item { Entrance { ScreenHeader("Cuentas") } }
        item {
            Entrance(delayMs = 60) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MoniColors.hero).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Disponible", color = MoniColors.heroFg, style = MaterialTheme.typography.labelMedium)
                    Text(soles(available), color = Color.White, style = MaterialTheme.typography.headlineMedium.tabular)
                    if (hasCards) {
                        Spacer(Modifier.height(6.dp))
                        Text("Deuda de tarjetas", color = MoniColors.heroFg, style = MaterialTheme.typography.labelMedium)
                        Text(soles(cardDebt), color = Color.White, style = MaterialTheme.typography.titleMedium.tabular)
                    }
                }
            }
        }
        item {
            Entrance(delayMs = 100) {
                PrimaryButton("Nueva cuenta", onClick = { vm.clearError(); form = FormTarget.New }, icon = LucideR.drawable.lucide_ic_plus)
            }
        }
        if (state.error != null && form == null) item { AlertBanner(state.error) }
        if (state.loaded && active.isEmpty()) {
            item {
                Text(
                    "Aún no tienes cuentas. Crea la primera para empezar.",
                    color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
        items(active, key = { it.id }) { a ->
            AccountCard(a, state.balances[a.id] ?: a.openingBalance) { vm.clearError(); form = FormTarget.Edit(a) }
        }
        if (archived.isNotEmpty()) {
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
                            ArchivedRow(a, enabled = !state.busy) { vm.setArchived(a.id, false, onAccountsChanged) }
                        }
                    }
                }
            }
        }
    }

    form?.let { target ->
        val account = (target as? FormTarget.Edit)?.account
        AccountFormSheet(
            account = account,
            busy = state.busy,
            error = state.error,
            onSave = { input -> vm.save(account?.id, input) { form = null; onAccountsChanged() } },
            onArchive = { account?.let { vm.setArchived(it.id, true) { form = null; onAccountsChanged() } } },
            onDismiss = { form = null },
        )
    }
}

private fun Modifier.heightMin() = this.then(Modifier.defaultMinSize(minHeight = 48.dp))

private fun Modifier.rotateBy(deg: Float) = this.then(Modifier.graphicsLayer { rotationZ = deg })

@Composable
private fun AccountCard(a: Account, balance: Double, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    val card = a.type == "credit_card"
    Column(
        Modifier
            .fillMaxWidth()
            .pressScale(source)
            .shadow(2.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(shape)
            .background(MoniColors.card)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .animateContentSize(moniSpring()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MoniColors.iconTile),
                contentAlignment = Alignment.Center,
            ) { LucideIcon(accountTypeIcon(a.type), null, tint = MoniColors.iconStroke, size = 22) }
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
            Column(horizontalAlignment = Alignment.End) {
                if (card) Text("Deuda", color = MoniColors.mut, style = MaterialTheme.typography.labelSmall)
                Text(
                    soles(balance),
                    color = if (card) MoniColors.exp else if (balance < 0) MoniColors.exp else MoniColors.fg,
                    style = MaterialTheme.typography.titleSmall.tabular,
                )
            }
        }
        if (card) CardUsage(a, balance)
    }
}

@Composable
private fun CardUsage(a: Account, debt: Double) {
    val limit = a.creditLimit
    if (limit != null && limit > 0) {
        val fraction = (debt / limit).toFloat().coerceIn(0f, 1f)
        val animated by animateFloatAsState(fraction, moniSpring(), label = "usage")
        Row {
            Text("Disponible", color = MoniColors.mut, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(soles(limit - debt), color = MoniColors.fg, style = MaterialTheme.typography.bodySmall.tabular)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MoniColors.track)) {
            Box(
                Modifier.fillMaxWidth(animated).height(6.dp).clip(CircleShape)
                    .background(if (fraction >= 0.9f) MoniColors.exp else MoniColors.acc),
            )
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
private fun ArchivedRow(a: Account, enabled: Boolean, onRestore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MoniColors.card).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(accountTypeIcon(a.type), null, tint = MoniColors.mut)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.name, color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(accountTypeLabel(a.type), color = MoniColors.mut, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onRestore, enabled = enabled, modifier = Modifier.height(48.dp)) {
            LucideIcon(LucideR.drawable.lucide_ic_archive_restore, null, tint = MoniColors.iconStroke, size = 16)
            Spacer(Modifier.width(6.dp))
            Text("Restaurar", color = MoniColors.iconStroke)
        }
    }
}
