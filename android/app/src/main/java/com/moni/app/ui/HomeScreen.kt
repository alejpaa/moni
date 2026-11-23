package com.moni.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moni.app.data.Transaction
import com.moni.app.data.summary
import com.moni.app.ui.theme.expenseColor
import com.moni.app.ui.theme.incomeColor
import java.text.NumberFormat
import java.util.Locale

private val money = NumberFormat.getNumberInstance(Locale.forLanguageTag("es")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onAdd: (String, Double, String, String?) -> Unit,
    onDelete: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val summary = state.transactions.summary()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tus finanzas") },
                actions = { TextButton(onClick = onSignOut) { Text("Salir") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Balance", style = MaterialTheme.typography.labelMedium)
                        Text(money.format(summary.balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("+" + money.format(summary.income), color = incomeColor())
                            Text("−" + money.format(summary.expense), color = expenseColor())
                        }
                    }
                }
            }
            item { AddForm(onAdd) }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.transactions.isEmpty()) {
                item { Text("Aún no hay movimientos.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.transactions, key = { it.id }) { t ->
                TransactionRow(t, onDelete)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AddForm(onAdd: (String, Double, String, String?) -> Unit) {
    var type by rememberSaveable { mutableStateOf("expense") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == "expense", onClick = { type = "expense" }, label = { Text("Gasto") })
                FilterChip(selected = type == "income", onClick = { type = "income" }, label = { Text("Ingreso") })
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val value = amount.replace(',', '.').toDoubleOrNull()
                    if (value != null && value > 0 && category.isNotBlank()) {
                        onAdd(type, value, category.trim(), note.trim().ifBlank { null })
                        amount = ""; category = ""; note = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Añadir") }
        }
    }
}

@Composable
private fun TransactionRow(t: Transaction, onDelete: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(t.category, fontWeight = FontWeight.Medium)
            Text(
                t.occurredAt.take(10) + (t.note?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            (if (t.isIncome) "+" else "−") + money.format(t.amount),
            color = if (t.isIncome) incomeColor() else expenseColor(),
        )
        TextButton(onClick = { onDelete(t.id) }) { Text("✕") }
    }
}
