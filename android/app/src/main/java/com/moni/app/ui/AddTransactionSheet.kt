package com.moni.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Account
import com.moni.app.data.Category
import com.moni.app.data.LIMA_ZONE
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class Frequency(val apiValue: String, val label: String) {
    Weekly("weekly", "Semanal"),
    Monthly("monthly", "Mensual"),
    Yearly("yearly", "Anual"),
}

private fun nextPeriodDate(base: LocalDate, freq: Frequency): LocalDate = when (freq) {
    Frequency.Weekly -> base.plusWeeks(1)
    Frequency.Monthly -> base.plusMonths(1)
    Frequency.Yearly -> base.plusYears(1)
}

/**
 * "Nuevo movimiento" bottom sheet. [onAdd] follows MoniViewModel.add: its last
 * argument must be invoked on success so the sheet can close.
 */
@Composable
internal fun AddTransactionSheet(
    state: UiState,
    onAdd: (String, Double, String, String?, String?, RecurringInput?, () -> Unit) -> Unit,
    onRetryCategories: () -> Unit,
    onDismiss: () -> Unit,
) {
    MoniBottomSheet(onDismiss = onDismiss, busy = state.busy) {
        AddSheetContent(
            busy = state.busy,
            error = state.sheetError,
            categories = state.categories,
            categoriesLoaded = state.categoriesLoaded,
            categoriesError = state.categoriesError,
            accounts = state.accounts,
            onRetryCategories = onRetryCategories,
            onSubmit = { type, amount, categoryId, accountId, note, recurring ->
                onAdd(type, amount, categoryId, accountId, note, recurring, onDismiss)
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddSheetContent(
    busy: Boolean,
    error: String?,
    categories: List<Category>,
    categoriesLoaded: Boolean,
    categoriesError: String?,
    accounts: List<Account>,
    onRetryCategories: () -> Unit,
    onSubmit: (String, Double, String, String?, String?, RecurringInput?) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(0) } // 0 = Gasto, 1 = Ingreso
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var accountPick by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var isRecurring by rememberSaveable { mutableStateOf(false) }
    var recurringAutoSet by rememberSaveable { mutableStateOf(false) }
    var recurringManuallyToggled by rememberSaveable { mutableStateOf(false) }
    var frequencyIndex by rememberSaveable { mutableStateOf(Frequency.Monthly.ordinal) }
    var nextDateIso by rememberSaveable { mutableStateOf(nextPeriodDate(LocalDate.now(LIMA_ZONE), Frequency.Monthly).toString()) }
    var dateCustomized by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val kind = if (type == 1) "income" else "expense"
    val shown = remember(categories, kind) { categories.filter { it.kind == kind } }
    // Switching Gasto/Ingreso drops a category of the other kind.
    val category = shown.firstOrNull { it.id == categoryId }
    // Default: the first non-archived account (Efectivo is seeded per workspace).
    val accountId = accountPick ?: accounts.firstOrNull { !it.archived }?.id

    val (value, amountValidationError) = validateAmount(raw = amount)
    val amountError = if (submitted) amountValidationError else null
    val categoryError = if (submitted && category == null) "Elige una categoría" else null

    fun submit() {
        submitted = true
        if (busy || value == null || category == null) return
        val cleanNote = note.trim().ifBlank { null }
        val recurring = if (isRecurring) {
            RecurringInput(
                title = cleanNote ?: category.name,
                frequency = Frequency.entries[frequencyIndex].apiValue,
                dueDate = nextDateIso,
            )
        } else null
        onSubmit(kind, value, category.id, accountId, cleanNote, recurring)
    }

    SheetColumn {
        Text("Nuevo movimiento", color = MoniColors.fg, style = MaterialTheme.typography.titleLarge)
        SegmentedControl(
            listOf("Gasto", "Ingreso"),
            type,
            { nextType ->
                if (!busy && nextType != type) {
                    type = nextType
                    if (!recurringManuallyToggled && recurringAutoSet) {
                        isRecurring = false
                        recurringAutoSet = false
                    }
                }
            },
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Monto") },
            prefix = { Text("S/ ") },
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
            categoriesLoaded = categoriesLoaded,
            categoriesError = categoriesError,
            income = type == 1,
            selectedId = category?.id,
            enabled = !busy,
            onRetryCategories = onRetryCategories,
            onSelect = { id ->
                categoryId = id
                if (!recurringManuallyToggled) {
                    val picked = shown.firstOrNull { it.id == id }?.name?.let(::normalizeCategory)
                    if (picked == "suscripciones" || picked == "sueldo") {
                        isRecurring = true
                        recurringAutoSet = true
                    } else if (recurringAutoSet) {
                        isRecurring = false
                        recurringAutoSet = false
                    }
                }
            },
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
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(FieldShape)
                .clickable(enabled = !busy, role = Role.Switch) {
                    recurringManuallyToggled = true
                    recurringAutoSet = false
                    isRecurring = !isRecurring
                }
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LucideIcon(LucideR.drawable.lucide_ic_repeat, null, tint = if (isRecurring) MoniColors.iconStroke else MoniColors.mut)
            Spacer(Modifier.width(10.dp))
            Text("Es recurrente", color = MoniColors.fg, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = isRecurring,
                onCheckedChange = if (busy) null else {
                    { checked ->
                        recurringManuallyToggled = true
                        recurringAutoSet = false
                        isRecurring = checked
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MoniColors.accFg,
                    checkedTrackColor = MoniColors.acc,
                    uncheckedThumbColor = MoniColors.mut,
                    uncheckedTrackColor = MoniColors.track,
                    uncheckedBorderColor = MoniColors.line,
                ),
            )
        }
        AnimatedVisibility(
            visible = isRecurring,
            enter = expandVertically(moniSpring()) + fadeIn(),
            exit = shrinkVertically(moniSpring()) + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Frecuencia", color = MoniColors.mut, style = MaterialTheme.typography.labelMedium)
                SegmentedControl(
                    options = Frequency.entries.map { it.label },
                    selected = frequencyIndex,
                    onSelect = { idx ->
                        if (!busy) {
                            frequencyIndex = idx
                            if (!dateCustomized) {
                                nextDateIso = nextPeriodDate(LocalDate.now(LIMA_ZONE), Frequency.entries[idx]).toString()
                            }
                        }
                    },
                )
                val dateLabel = if (type == 1) "Próximo ingreso" else "Próximo cobro"
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formatDate(nextDateIso),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(dateLabel) },
                        trailingIcon = {
                            LucideIcon(LucideR.drawable.lucide_ic_calendar, null, tint = MoniColors.mut)
                        },
                        singleLine = true,
                        enabled = !busy,
                        shape = FieldShape,
                        colors = moniFieldColors(),
                        textStyle = MaterialTheme.typography.bodyLarge.tabular,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(FieldShape)
                            .clickable(enabled = !busy, role = Role.Button) { showDatePicker = true },
                    )
                }
            }
        }
        AlertBanner(error)
        PrimaryButton(
            text = if (busy) "Guardando…" else "Guardar",
            onClick = { submit() },
            enabled = !busy,
            loading = busy,
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(nextDateIso) {
            runCatching { LocalDate.parse(nextDateIso) }.getOrElse { LocalDate.now(LIMA_ZONE) }
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MoniColors.card),
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            nextDateIso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                            dateCustomized = true
                        }
                        showDatePicker = false
                    },
                ) { Text("Aceptar", color = MoniColors.iconStroke) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = MoniColors.mut) }
            },
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MoniColors.card,
                    titleContentColor = MoniColors.fg,
                    headlineContentColor = MoniColors.fg,
                    selectedDayContainerColor = MoniColors.acc,
                    selectedDayContentColor = MoniColors.accFg,
                    todayDateBorderColor = MoniColors.acc,
                    todayContentColor = MoniColors.iconStroke,
                ),
            )
        }
    }
}

/**
 * Category chips for both kinds laid out in the same Box, so the block always
 * measures as the taller of the two and the sheet never resizes when toggling
 * Gasto/Ingreso.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<Category>,
    categoriesLoaded: Boolean,
    categoriesError: String?,
    income: Boolean,
    selectedId: String?,
    enabled: Boolean,
    onRetryCategories: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val expenseCategories = remember(categories) { categories.filter { it.kind == "expense" } }
    val incomeCategories = remember(categories) { categories.filter { it.kind == "income" } }

    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        if (categories.isNotEmpty()) {
            listOf(false to expenseCategories, true to incomeCategories).forEach { (isIncome, list) ->
                val active = isIncome == income
                val alpha by animateFloatAsState(if (active) 1f else 0f, tween(200), label = "chipsAlpha")
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(if (active) 1f else 0f)
                        .alpha(alpha)
                        .then(if (active) Modifier else Modifier.clearAndSetSemantics { }),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    list.forEach { c ->
                        ChoiceChip(c.name, categoryIcon(c, isIncome), active && c.id == selectedId, enabled && active) { onSelect(c.id) }
                    }
                }
            }
        } else if (categoriesError != null) {
            AlertBanner(
                message = categoriesError,
                actionLabel = "Reintentar",
                onAction = onRetryCategories,
            )
        } else if (categoriesLoaded) {
            Text(
                "No hay categorías activas",
                color = MoniColors.mut,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        } else {
            Text(
                "Cargando categorías…",
                color = MoniColors.mut,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}
