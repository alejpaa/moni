package com.moni.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Account
import com.moni.app.data.AccountInput
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular

/** Create (account == null) or edit sheet. */
@Composable
internal fun AccountFormSheet(
    account: Account?,
    busy: Boolean,
    error: String?,
    onSave: (AccountInput) -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    MoniBottomSheet(onDismiss = onDismiss, busy = busy) {
        AccountFormContent(account, busy, error, onSave, onArchive)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountFormContent(
    account: Account?,
    busy: Boolean,
    error: String?,
    onSave: (AccountInput) -> Unit,
    onArchive: () -> Unit,
) {
    val formKey = account?.id ?: "new"
    var name by rememberSaveable(formKey) { mutableStateOf(account?.name ?: "") }
    var type by rememberSaveable(formKey) { mutableStateOf(account?.type ?: "debit") }
    var institution by rememberSaveable(formKey) { mutableStateOf(account?.institution ?: "") }
    var opening by rememberSaveable(formKey) { mutableStateOf(if (account != null) plainDecimal(account.openingBalance) else "") }
    var last4 by rememberSaveable(formKey) { mutableStateOf(account?.last4 ?: "") }
    var limit by rememberSaveable(formKey) { mutableStateOf(plainDecimal(account?.creditLimit)) }
    var closing by rememberSaveable(formKey) { mutableStateOf(account?.closingDay?.toString() ?: "") }
    var due by rememberSaveable(formKey) { mutableStateOf(account?.dueDay?.toString() ?: "") }
    var submitted by rememberSaveable(formKey) { mutableStateOf(false) }
    var confirmArchive by rememberSaveable(formKey) { mutableStateOf(false) }

    val openingReveal = rememberRevealState()
    val limitReveal = rememberRevealState()

    val card = type == "credit_card"
    val (openingValue, openingValidationError) = validateAmount(
        raw = opening,
        allowNegative = !card,
        allowZero = true,
        allowBlankAsZero = true,
    )
    val (limitValue, limitValidationError) = if (card) {
        validateAmount(raw = limit, allowNegative = false, allowZero = false, allowBlankAsZero = false)
    } else {
        null to null
    }
    val closingValue = closing.trim().toIntOrNull()
    val dueValue = due.trim().toIntOrNull()

    val nameError = if (submitted && name.isBlank()) "Escribe un nombre" else null
    val openingError = if (submitted) openingValidationError else null
    val last4Error = if (submitted && card && !Regex("^[0-9]{4}$").matches(last4.trim())) "Deben ser 4 dígitos" else null
    val limitError = if (submitted && card) limitValidationError else null
    val closingError = if (submitted && card && (closingValue == null || closingValue !in 1..31)) "Del 1 al 31" else null
    val dueError = if (submitted && card && (dueValue == null || dueValue !in 1..31)) "Del 1 al 31" else null

    fun submit() {
        submitted = true
        if (busy) return
        if (name.isBlank() || openingValue == null) return
        if (card && (!Regex("^[0-9]{4}$").matches(last4.trim()) || limitValue == null ||
                closingValue == null || closingValue !in 1..31 || dueValue == null || dueValue !in 1..31)) return
        onSave(
            AccountInput(
                name = name.trim(),
                type = type,
                openingBalance = openingValue,
                institution = institution.trim().ifBlank { null },
                last4 = if (card) last4.trim() else null,
                creditLimit = if (card) limitValue else null,
                closingDay = if (card) closingValue else null,
                dueDay = if (card) dueValue else null,
            ),
        )
    }

    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
    val number = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)

    SheetColumn {
        Text(
            if (account == null) "Nueva cuenta" else "Editar cuenta",
            color = MoniColors.fg, style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nombre") }, singleLine = true, enabled = !busy,
            isError = nameError != null, shape = FieldShape, colors = moniFieldColors(),
            supportingText = nameError?.let { { FieldMessage(it, true) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Tipo", color = MoniColors.mut, style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            accountTypes.forEach { t ->
                ChoiceChip(accountTypeLabel(t), accountTypeIcon(t), t == type, !busy) { type = t }
            }
        }
        OutlinedTextField(
            value = institution, onValueChange = { institution = it },
            label = { Text("Banco o entidad (opcional)") }, singleLine = true, enabled = !busy,
            shape = FieldShape, colors = moniFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = opening, onValueChange = { opening = it },
            label = { Text(if (card) "Deuda actual" else "Saldo inicial") },
            prefix = { Text("S/ ") }, singleLine = true, enabled = !busy,
            isError = openingError != null, shape = FieldShape, colors = moniFieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge.tabular,
            visualTransformation = if (openingReveal.effectiveHidden && opening.isNotEmpty()) PasswordVisualTransformation('•') else VisualTransformation.None,
            trailingIcon = if (openingReveal.globalHidden) {
                {
                    IconButton(
                        onClick = openingReveal.toggle,
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        LucideIcon(
                            if (openingReveal.effectiveHidden) LucideR.drawable.lucide_ic_eye else LucideR.drawable.lucide_ic_eye_off,
                            if (openingReveal.effectiveHidden) "Mostrar monto" else "Ocultar monto",
                            tint = MoniColors.mut,
                            size = 20,
                        )
                    }
                }
            } else null,
            supportingText = openingError?.let { { FieldMessage(it, true) } },
            keyboardOptions = decimal,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(
            visible = card,
            enter = expandVertically(moniSpring()) + fadeIn(),
            exit = shrinkVertically(moniSpring()) + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = last4, onValueChange = { last4 = it.filter(Char::isDigit).take(4) },
                    label = { Text("Últimos 4 dígitos") }, singleLine = true, enabled = !busy,
                    isError = last4Error != null, shape = FieldShape, colors = moniFieldColors(),
                    supportingText = last4Error?.let { { FieldMessage(it, true) } },
                    keyboardOptions = number,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = limit, onValueChange = { limit = it },
                    label = { Text("Línea de crédito") }, prefix = { Text("S/ ") },
                    singleLine = true, enabled = !busy,
                    isError = limitError != null, shape = FieldShape, colors = moniFieldColors(),
                    textStyle = MaterialTheme.typography.bodyLarge.tabular,
                    visualTransformation = if (limitReveal.effectiveHidden && limit.isNotEmpty()) PasswordVisualTransformation('•') else VisualTransformation.None,
                    trailingIcon = if (limitReveal.globalHidden) {
                        {
                            IconButton(
                                onClick = limitReveal.toggle,
                                modifier = Modifier.focusProperties { canFocus = false },
                            ) {
                                LucideIcon(
                                    if (limitReveal.effectiveHidden) LucideR.drawable.lucide_ic_eye else LucideR.drawable.lucide_ic_eye_off,
                                    if (limitReveal.effectiveHidden) "Mostrar monto" else "Ocultar monto",
                                    tint = MoniColors.mut,
                                    size = 20,
                                )
                            }
                        }
                    } else null,
                    supportingText = limitError?.let { { FieldMessage(it, true) } },
                    keyboardOptions = decimal,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = closing, onValueChange = { closing = it.filter(Char::isDigit).take(2) },
                        label = { Text("Día de cierre") }, singleLine = true, enabled = !busy,
                        isError = closingError != null, shape = FieldShape, colors = moniFieldColors(),
                        supportingText = closingError?.let { { FieldMessage(it, true) } },
                        keyboardOptions = number,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = due, onValueChange = { due = it.filter(Char::isDigit).take(2) },
                        label = { Text("Día de pago") }, singleLine = true, enabled = !busy,
                        isError = dueError != null, shape = FieldShape, colors = moniFieldColors(),
                        supportingText = dueError?.let { { FieldMessage(it, true) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        AlertBanner(error)
        PrimaryButton(
            text = if (busy) "Guardando…" else "Guardar",
            onClick = { submit() }, enabled = !busy, loading = busy,
        )
        if (account != null) {
            TextButton(
                onClick = { confirmArchive = true }, enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Archivar cuenta", color = MoniColors.exp) }
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            containerColor = MoniColors.card,
            title = { Text("¿Archivar cuenta?") },
            text = { Text("\"${account?.name}\" dejará de aparecer al registrar movimientos. Podrás restaurarla cuando quieras.") },
            confirmButton = {
                TextButton(onClick = { confirmArchive = false; onArchive() }) { Text("Archivar", color = MoniColors.exp) }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },
        )
    }
}
