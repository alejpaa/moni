package com.moni.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moni.app.data.Account
import com.moni.app.data.Category
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular

/**
 * "Nuevo movimiento" bottom sheet. [onAdd] follows MoniViewModel.add: its last
 * argument must be invoked on success so the sheet can close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTransactionSheet(
    state: UiState,
    onAdd: (String, Double, String, String?, String?, () -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!state.busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MoniColors.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        AddSheetContent(
            busy = state.busy,
            error = state.error,
            categories = state.categories,
            accounts = state.accounts,
            onSubmit = { type, amount, categoryId, accountId, note ->
                onAdd(type, amount, categoryId, accountId, note, onDismiss)
            },
        )
    }
}

/** Selectable chip (icon + name), >= 48dp tall, color-animated like the filter pills. */
@Composable
internal fun ChoiceChip(label: String, icon: Int?, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSheetContent(
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
        CategoryChips(
            categories = categories,
            income = type == 1,
            selectedId = category?.id,
            enabled = !busy,
            onSelect = { categoryId = it },
        )
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

/**
 * Category chips for both kinds laid out in the same Box, so the block always
 * measures as the taller of the two and the sheet never resizes when toggling
 * Gasto/Ingreso. The inactive kind is transparent, disabled and hidden from
 * accessibility; the two crossfade. While categories load, the block reserves
 * one chip row and shows a placeholder instead of collapsing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<Category>,
    income: Boolean,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        listOf(false, true).forEach { isIncome ->
            val active = isIncome == income
            val alpha by animateFloatAsState(if (active) 1f else 0f, tween(200), label = "chipsAlpha")
            val kind = if (isIncome) "income" else "expense"
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    // Active set on top so the invisible one never intercepts taps.
                    .zIndex(if (active) 1f else 0f)
                    .alpha(alpha)
                    .then(if (active) Modifier else Modifier.clearAndSetSemantics { }),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                categories.filter { it.kind == kind }.forEach { c ->
                    ChoiceChip(c.name, categoryIcon(c, isIncome), active && c.id == selectedId, enabled && active) { onSelect(c.id) }
                }
            }
        }
        if (categories.isEmpty()) {
            Text(
                "Cargando categorías…",
                color = MoniColors.mut,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}
