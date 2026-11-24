package com.moni.app.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.data.Category
import com.moni.app.ui.theme.MoniColors

/** Body of the "Gestionar categorías" sheet: list <-> form, inside the same sheet. */
@Composable
internal fun CategoriesContent(
    categories: List<Category>,
    isViewer: Boolean,
    busy: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onCreate: (name: String, kind: String, icon: String, onDone: () -> Unit) -> Unit,
    onUpdate: (id: String, name: String, icon: String, onDone: () -> Unit) -> Unit,
    onSetArchived: (id: String, archived: Boolean, onDone: () -> Unit) -> Unit,
) {
    var kindIdx by rememberSaveable { mutableStateOf(0) }
    var editingOpen by rememberSaveable { mutableStateOf(false) }
    var editingCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val kind = if (kindIdx == 0) "expense" else "income"

    val targetCategory = editingCategoryId?.let { id -> categories.find { it.id == id } }
    if (!editingOpen || isViewer) {
        CategoryList(
            categories = categories,
            isViewer = isViewer,
            busy = busy,
            error = error,
            kindIdx = kindIdx,
            onKind = { kindIdx = it },
            onNew = { onClearError(); editingCategoryId = null; editingOpen = true },
            onEdit = { c -> onClearError(); editingCategoryId = c.id; editingOpen = true },
            onRestore = { c -> onSetArchived(c.id, false) {} },
        )
    } else {
        CategoryForm(
            busy = busy,
            error = error,
            category = targetCategory,
            initialKind = kind,
            onBack = { onClearError(); editingOpen = false; editingCategoryId = null },
            onSave = { name, k, icon ->
                if (targetCategory == null) {
                    onCreate(name, k, icon) { editingOpen = false; editingCategoryId = null }
                } else {
                    onUpdate(targetCategory.id, name, icon) { editingOpen = false; editingCategoryId = null }
                }
            },
            onArchive = { c -> onSetArchived(c.id, true) { editingOpen = false; editingCategoryId = null } },
        )
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    isViewer: Boolean,
    busy: Boolean,
    error: String?,
    kindIdx: Int,
    onKind: (Int) -> Unit,
    onNew: () -> Unit,
    onEdit: (Category) -> Unit,
    onRestore: (Category) -> Unit,
) {
    val kind = if (kindIdx == 0) "expense" else "income"
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val active = remember(categories, kind) { categories.filter { it.kind == kind && !it.archived } }
    val archived = remember(categories, kind) { categories.filter { it.kind == kind && it.archived } }
    SheetColumn {
        Text("Categorías", color = MoniColors.fg, style = MaterialTheme.typography.titleMedium)
        SegmentedControl(listOf("Gasto", "Ingreso"), kindIdx, onKind)
        AlertBanner(error)
        Column(Modifier.fillMaxWidth().heightIn(min = 320.dp)) {
            if (active.isEmpty()) {
                Text(
                    "No hay categorías de este tipo", color = MoniColors.mut,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            active.forEach { c ->
                CategoryRow(c, onClick = if (isViewer) null else { { onEdit(c) } })
            }
            if (archived.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { showArchived = !showArchived },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Archivadas (${archived.size})", color = MoniColors.mut, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    LucideIcon(
                        if (showArchived) LucideR.drawable.lucide_ic_chevron_up else LucideR.drawable.lucide_ic_chevron_down,
                        null, tint = MoniColors.mut,
                    )
                }
                AnimatedVisibility(showArchived) {
                    Column {
                        archived.forEach { c ->
                            CategoryRow(c, onClick = null, dim = true) {
                                if (!isViewer) {
                                    TextButton(onClick = { onRestore(c) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("Restaurar", color = MoniColors.iconStroke)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!isViewer) {
            PrimaryButton("Nueva categoría", onClick = onNew, icon = LucideR.drawable.lucide_ic_plus)
        }
    }
}

@Composable
private fun CategoryRow(c: Category, onClick: (() -> Unit)?, dim: Boolean = false, trailing: @Composable () -> Unit = {}) {
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            icon = categoryIcon(c, c.kind == "income"),
            size = 40.dp,
            radius = 14.dp,
            iconSize = 20,
            tint = if (dim) MoniColors.mut else MoniColors.iconStroke,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            c.name, color = if (dim) MoniColors.mut else MoniColors.fg,
            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
        )
        if (onClick != null) LucideIcon(LucideR.drawable.lucide_ic_chevron_right, null, tint = MoniColors.mut)
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryForm(
    busy: Boolean,
    error: String?,
    category: Category?,
    initialKind: String,
    onBack: () -> Unit,
    onSave: (name: String, kind: String, icon: String) -> Unit,
    onArchive: (Category) -> Unit,
) {
    val formKey = category?.id ?: "new-$initialKind"
    val isNew = category == null
    var name by rememberSaveable(formKey) { mutableStateOf(category?.name.orEmpty()) }
    var kindIdx by rememberSaveable(formKey) { mutableStateOf(if ((category?.kind ?: initialKind) == "income") 1 else 0) }
    var icon by rememberSaveable(formKey) {
        mutableStateOf(
            category?.icon?.takeIf { it in iconKeys } ?: (if (kindIdx == 1) "arrow-down-left" else "arrow-up-right"),
        )
    }
    var confirmArchive by rememberSaveable(formKey) { mutableStateOf(false) }
    val kind = if (kindIdx == 1) "income" else "expense"

    SheetColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isNew) "Nueva categoría" else "Editar categoría", color = MoniColors.fg,
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Volver", color = MoniColors.iconStroke)
            }
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it }, label = { Text("Nombre") },
            singleLine = true, enabled = !busy, shape = FieldShape, colors = moniFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (isNew) {
            Text("Tipo", color = MoniColors.mut, style = MaterialTheme.typography.labelLarge)
            SegmentedControl(listOf("Gasto", "Ingreso"), kindIdx, { kindIdx = it })
        }
        Text("Icono", color = MoniColors.mut, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            iconKeys.forEach { (key, res) ->
                val selected = key == icon
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .size(48.dp)
                        .pressScale(source)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) MoniColors.acc else MoniColors.iconTile)
                        .selectable(selected = selected, enabled = !busy, interactionSource = source, indication = null, role = Role.RadioButton) { icon = key },
                    contentAlignment = Alignment.Center,
                ) { LucideIcon(res, key, tint = if (selected) MoniColors.accFg else MoniColors.iconStroke, size = 22) }
            }
        }
        AlertBanner(error)
        PrimaryButton(
            if (busy) "Guardando…" else "Guardar",
            onClick = { onSave(name, kind, icon) },
            enabled = !busy && name.isNotBlank(), loading = busy,
        )
        if (category != null) {
            TextButton(
                onClick = { confirmArchive = true }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                LucideIcon(LucideR.drawable.lucide_ic_archive, null, tint = MoniColors.exp, size = 16)
                Spacer(Modifier.width(8.dp))
                Text("Archivar", color = MoniColors.exp)
            }
        }
    }

    if (confirmArchive && category != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            containerColor = MoniColors.card,
            title = { Text("¿Archivar \"${category.name}\"?", color = MoniColors.fg) },
            text = {
                Text(
                    "Dejará de aparecer al registrar movimientos. Tus movimientos anteriores se conservan y puedes restaurarla cuando quieras.",
                    color = MoniColors.mut,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmArchive = false; onArchive(category) }) { Text("Archivar", color = MoniColors.exp) }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar", color = MoniColors.fg) } },
        )
    }
}
