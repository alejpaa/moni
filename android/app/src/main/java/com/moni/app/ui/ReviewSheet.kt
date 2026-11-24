package com.moni.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Account
import com.moni.app.data.Category
import com.moni.app.data.Transaction
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular

@Composable
internal fun PendingEntry(count: Int, onClick: () -> Unit) {
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

/** Bottom sheet that lists pending (auto-captured) movements to confirm or ignore. */
@Composable
internal fun ReviewSheet(
    state: UiState,
    onConfirm: (String, String?, String?) -> Unit,
    onIgnore: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    MoniBottomSheet(onDismiss = onDismiss) {
        ReviewContent(
            pending = state.pending,
            categories = state.categories,
            accounts = state.accounts,
            workingIds = state.reviewWorkingIds,
            error = state.sheetError,
            onConfirm = onConfirm,
            onIgnore = onIgnore,
        )
    }
}

@Composable
private fun ReviewContent(
    pending: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    workingIds: Set<String>,
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            itemsIndexed(pending, key = { _, t -> t.id }) { _, t ->
                ReviewItem(
                    t = t,
                    categories = categories,
                    accounts = accounts,
                    working = t.id in workingIds,
                    onConfirm = onConfirm,
                    onIgnore = onIgnore,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ReviewItem(
    t: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    working: Boolean,
    onConfirm: (String, String?, String?) -> Unit,
    onIgnore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryId by rememberSaveable(t.id) { mutableStateOf(t.categoryId) }
    var accountId by rememberSaveable(t.id) { mutableStateOf(t.accountId) }
    val kindCategories = remember(categories, t.type) { categories.filter { it.kind == t.type } }
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
            val reveal = rememberRevealState()
            Text(
                formatSignedMoney(t.amount, t.isIncome, reveal.effectiveHidden),
                color = if (t.isIncome) MoniColors.inc else MoniColors.exp,
                style = MaterialTheme.typography.titleSmall.tabular.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.revealOnTap(reveal, label = title),
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
                    .clickable(enabled = !working, role = Role.Button) { onIgnore(t.id) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Ignorar", color = MoniColors.mut, style = MaterialTheme.typography.labelLarge) }
            PrimaryButton(
                "Confirmar",
                onClick = { onConfirm(t.id, categoryId, accountId) },
                enabled = !working,
                loading = working,
                icon = LucideR.drawable.lucide_ic_check,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
