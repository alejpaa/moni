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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moni.app.data.Account
import com.moni.app.data.AccountInput
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.tabular

private fun parseMoney(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

private fun plain(v: Double?): String =
    if (v == null) "" else java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()

/** Create (account == null) or edit sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountFormSheet(
    account: Account?,
    busy: Boolean,
    error: String?,
    onSave: (AccountInput) -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MoniColors.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
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
    var name by rememberSaveable { mutableStateOf(account?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(account?.type ?: "debit") }
    var institution by rememberSaveable { mutableStateOf(account?.institution ?: "") }
    var opening by rememberSaveable { mutableStateOf(if (account != null) plain(account.openingBalance) else "") }
    var last4 by rememberSaveable { mutableStateOf(account?.last4 ?: "") }
    var limit by rememberSaveable { mutableStateOf(plain(account?.creditLimit)) }
    var closing by rememberSaveable { mutableStateOf(account?.closingDay?.toString() ?: "") }
    var due by rememberSaveable { mutableStateOf(account?.dueDay?.toString() ?: "") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }

    val card = type == "credit_card"
    val openingValue = if (opening.isBlank()) 0.0 else parseMoney(opening)
    val limitValue = parseMoney(limit)
    val closingValue = closing.trim().toIntOrNull()
    val dueValue = due.trim().toIntOrNull()

    val nameError = if (submitted && name.isBlank()) "Escribe un nombre" else null
    val openingError = if (submitted && (openingValue == null || openingValue < 0 && card)) "Introduce un monto válido" else null
    val last4Error = if (submitted && card && !Regex("^[0-9]{4}$").matches(last4.trim())) "Deben ser 4 dígitos" else null
    val limitError = if (submitted && card && (limitValue == null || limitValue <= 0)) "Introduce una línea mayor que 0" else null
    val closingError = if (submitted && card && (closingValue == null || closingValue !in 1..31)) "Del 1 al 31" else null
    val dueError = if (submitted && card && (dueValue == null || dueValue !in 1..31)) "Del 1 al 31" else null

    fun submit() {
        submitted = true
        if (busy) return
        if (name.isBlank() || openingValue == null || (openingValue < 0 && card)) return
        if (card && (!Regex("^[0-9]{4}$").matches(last4.trim()) || limitValue == null || limitValue <= 0 ||
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

    Column(
        Modifier
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
