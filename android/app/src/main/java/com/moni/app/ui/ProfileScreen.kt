package com.moni.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R as LucideR
import com.moni.app.BuildConfig
import com.moni.app.data.CardDueNotifications
import com.moni.app.data.CardDueScheduler
import com.moni.app.data.Prefs
import com.moni.app.ui.theme.MoniColors

private enum class ProfileSheet { Name, Password, Categories }

/**
 * "Perfil" tab. Reads categories from the shared [state] store and only loads the profile once per session.
 */
@Composable
fun ProfileScreen(
    state: UiState,
    vm: ProfileViewModel,
    contentPadding: PaddingValues,
    onSignOut: () -> Unit,
    onClearSheetError: () -> Unit,
    onCreateCategory: (name: String, kind: String, icon: String, onDone: () -> Unit) -> Unit,
    onUpdateCategory: (id: String, name: String, icon: String, onDone: () -> Unit) -> Unit,
    onSetCategoryArchived: (id: String, archived: Boolean, onDone: () -> Unit) -> Unit,
) {
    val profileState by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.sessionKey) { vm.loadOnce(state.sessionKey) }

    var sheet by rememberSaveable { mutableStateOf<ProfileSheet?>(null) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val close = { sheet = null; vm.clearMessages(); onClearSheetError() }
    val open = { s: ProfileSheet -> vm.clearMessages(); onClearSheetError(); sheet = s }

    val name = profileState.profile?.fullName?.takeIf { it.isNotBlank() }
    val shownEmail = state.email ?: profileState.profile?.email
    val hideAmounts = LocalHideAmounts.current
    val appLockEnabled by Prefs.appLockEnabled.collectAsStateWithLifecycle()
    val cardNotifsEnabled by Prefs.cardNotificationsEnabled.collectAsStateWithLifecycle()
    val cardNotifyDays by Prefs.cardNotifyDaysBefore.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var bioStatus by remember { mutableStateOf(biometricStatus(context)) }
    var lockError by rememberSaveable { mutableStateOf<String?>(null) }
    var notifError by rememberSaveable { mutableStateOf<String?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            Prefs.setCardNotificationsEnabled(true)
            CardDueScheduler.sync(context, true)
            notifError = null
        } else {
            Prefs.setCardNotificationsEnabled(false)
            CardDueScheduler.sync(context, false)
            notifError = "Activa el permiso de notificaciones en Ajustes para recibir avisos"
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bioStatus = biometricStatus(context)
                if (Prefs.cardNotificationsEnabled.value && !CardDueNotifications.hasPermission(context)) {
                    Prefs.setCardNotificationsEnabled(false)
                    CardDueScheduler.sync(context, false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { Entrance { ScreenHeader("Perfil") } }
        profileState.loadError?.let { loadErr ->
            item(key = "profile-load-error") {
                AlertBanner(
                    message = loadErr,
                    actionLabel = "Reintentar",
                    onAction = { vm.loadOnce(state.sessionKey) },
                )
            }
        }
        item(key = "card") {
            Entrance(delayMs = 60) {
                ProfileCard(name = name, email = shownEmail, onEdit = { open(ProfileSheet.Name) })
            }
        }
        item(key = "privacy") {
            Entrance(delayMs = 100) {
                SettingsGroup("Privacidad") {
                    SettingsSwitchRow(
                        icon = if (hideAmounts) LucideR.drawable.lucide_ic_eye_off else LucideR.drawable.lucide_ic_eye,
                        title = "Ocultar montos",
                        subtitle = "Muestra S/ •••• en saldos y movimientos",
                        checked = hideAmounts,
                        onCheckedChange = { Prefs.setHideAmounts(it) },
                    )
                }
            }
        }
        item(key = "security") {
            Entrance(delayMs = 120) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsGroup("Seguridad") {
                        SettingsSwitchRow(
                            icon = LucideR.drawable.lucide_ic_fingerprint_pattern,
                            title = "Bloqueo con biometría",
                            subtitle = if (bioStatus.available) "Pide huella o bloqueo al abrir y tras 30 s fuera" else bioStatus.reason,
                            checked = appLockEnabled && bioStatus.available,
                            enabled = bioStatus.available,
                            onCheckedChange = { enable ->
                                lockError = null
                                if (!enable) {
                                    Prefs.setAppLockEnabled(false)
                                } else if (activity != null) {
                                    promptBiometric(
                                        activity = activity,
                                        title = "Activar bloqueo",
                                        subtitle = "Confirma tu identidad para proteger moni",
                                        onSuccess = { Prefs.setAppLockEnabled(true) },
                                        onError = { msg -> lockError = msg },
                                    )
                                }
                            },
                        )
                        HorizontalDivider(color = MoniColors.divider, modifier = Modifier.padding(start = 66.dp))
                        SettingsRow(LucideR.drawable.lucide_ic_lock, "Cambiar contraseña") { open(ProfileSheet.Password) }
                    }
                    AlertBanner(lockError)
                }
            }
        }
        item(key = "notifications") {
            Entrance(delayMs = 150) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsGroup("Notificaciones") {
                        SettingsSwitchRow(
                            icon = if (cardNotifsEnabled) LucideR.drawable.lucide_ic_bell else LucideR.drawable.lucide_ic_bell_off,
                            title = "Vencimiento de tarjetas",
                            subtitle = "Avisa antes del pago de tus tarjetas",
                            checked = cardNotifsEnabled,
                            onCheckedChange = { enable ->
                                notifError = null
                                if (!enable) {
                                    Prefs.setCardNotificationsEnabled(false)
                                    CardDueScheduler.sync(context, false)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !CardDueNotifications.hasPermission(context)
                                ) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Prefs.setCardNotificationsEnabled(true)
                                    CardDueScheduler.sync(context, true)
                                }
                            },
                        )
                        AnimatedVisibility(
                            visible = cardNotifsEnabled,
                            enter = expandVertically(moniSpring()) + fadeIn(),
                            exit = shrinkVertically(moniSpring()) + fadeOut(),
                        ) {
                            Column {
                                HorizontalDivider(color = MoniColors.divider, modifier = Modifier.padding(start = 66.dp))
                                DaysBeforeSelector(
                                    selectedDays = cardNotifyDays,
                                    onSelect = { days ->
                                        Prefs.setCardNotifyDaysBefore(days)
                                        CardDueScheduler.triggerCheckNow(context)
                                    },
                                )
                            }
                        }
                    }
                    AlertBanner(notifError)
                }
            }
        }
        item(key = "categories") {
            Entrance(delayMs = 180) {
                SettingsGroup("Categorías") {
                    SettingsRow(
                        LucideR.drawable.lucide_ic_tag,
                        if (state.isViewer) "Ver categorías" else "Gestionar categorías",
                    ) { open(ProfileSheet.Categories) }
                }
            }
        }
        item(key = "account") {
            Entrance(delayMs = 240) {
                SettingsGroup("Cuenta") {
                    SettingsRow(LucideR.drawable.lucide_ic_log_out, "Cerrar sesión", danger = true, chevron = false) {
                        confirmSignOut = true
                    }
                }
            }
        }
        item(key = "version") {
            Entrance(delayMs = 300) {
                Text(
                    "moni v${BuildConfig.VERSION_NAME}",
                    color = MoniColors.mut,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    when (sheet) {
        ProfileSheet.Name -> MoniBottomSheet(onDismiss = close, busy = profileState.busy) {
            EditNameContent(profileState, initial = name.orEmpty(), onSave = { n -> vm.saveName(n, close) })
        }
        ProfileSheet.Password -> MoniBottomSheet(onDismiss = close, busy = profileState.busy) {
            PasswordContent(profileState, onSave = { p, reset -> vm.changePassword(p, reset) })
        }
        ProfileSheet.Categories -> MoniBottomSheet(onDismiss = close, busy = state.busy) {
            CategoriesContent(
                categories = state.allCategories,
                isViewer = state.isViewer,
                busy = state.busy,
                error = state.sheetError,
                onClearError = onClearSheetError,
                onCreate = onCreateCategory,
                onUpdate = onUpdateCategory,
                onSetArchived = onSetCategoryArchived,
            )
        }
        null -> Unit
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            containerColor = MoniColors.card,
            title = { Text("¿Cerrar sesión?", color = MoniColors.fg) },
            text = { Text("Tendrás que volver a entrar con tu email y contraseña.", color = MoniColors.mut) },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Cerrar sesión", color = MoniColors.exp) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancelar", color = MoniColors.fg) } },
        )
    }
}

@Composable
private fun ProfileCard(name: String?, email: String?, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().cardShadow(shape).clip(shape).background(MoniColors.card).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(MoniColors.acc), contentAlignment = Alignment.Center) {
            Text(initials(name, email), color = MoniColors.accFg, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(8.dp))
        Text(name ?: "Sin nombre", color = if (name == null) MoniColors.mut else MoniColors.fg, style = MaterialTheme.typography.titleMedium)
        if (email != null) Text(email, color = MoniColors.mut, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
            LucideIcon(LucideR.drawable.lucide_ic_pencil, null, tint = MoniColors.iconStroke, size = 16)
            Spacer(Modifier.width(6.dp))
            Text("Editar nombre", color = MoniColors.iconStroke)
        }
    }
}

private fun initials(name: String?, email: String?): String {
    val parts = name?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    val s = when {
        parts.size >= 2 -> "${parts.first().first()}${parts[1].first()}"
        parts.size == 1 -> parts.first().take(2)
        else -> email?.firstOrNull()?.toString() ?: "?"
    }
    return s.uppercase()
}

private fun Modifier.cardShadow(shape: RoundedCornerShape) =
    shadow(6.dp, shape, ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(), color = MoniColors.mut, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 8.dp),
        )
        Column(Modifier.fillMaxWidth().cardShadow(shape).clip(shape).background(MoniColors.card)) { content() }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: Int,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, radius = 12.dp, tint = if (enabled) MoniColors.iconStroke else MoniColors.mut)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) MoniColors.fg else MoniColors.mut,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(subtitle, color = MoniColors.mut, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MoniColors.accFg,
                checkedTrackColor = MoniColors.acc,
                uncheckedThumbColor = MoniColors.mut,
                uncheckedTrackColor = MoniColors.track,
                uncheckedBorderColor = MoniColors.line,
                disabledUncheckedThumbColor = MoniColors.mut.copy(alpha = 0.5f),
                disabledUncheckedTrackColor = MoniColors.track,
                disabledUncheckedBorderColor = MoniColors.line,
            ),
        )
    }
}

private val notifyDayOptions = listOf(1, 2, 3, 5, 7)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DaysBeforeSelector(selectedDays: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Avisar con anticipación (y el mismo día)",
            color = MoniColors.mut,
            style = MaterialTheme.typography.labelMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            notifyDayOptions.forEach { days ->
                val label = if (days == 1) "1 día" else "$days días"
                ChoiceChip(
                    label = label,
                    icon = null,
                    selected = selectedDays == days,
                    onClick = { onSelect(days) },
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: Int, title: String, danger: Boolean = false, chevron: Boolean = true, onClick: () -> Unit) {
    val tone = if (danger) MoniColors.exp else MoniColors.fg
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            icon = icon,
            radius = 12.dp,
            tint = if (danger) MoniColors.exp else MoniColors.iconStroke,
            containerColor = if (danger) MoniColors.exp.copy(alpha = 0.10f) else MoniColors.iconTile,
        )
        Spacer(Modifier.width(14.dp))
        Text(title, color = tone, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (chevron) LucideIcon(LucideR.drawable.lucide_ic_chevron_right, null, tint = MoniColors.mut)
    }
}

@Composable
private fun EditNameContent(state: ProfileState, initial: String, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    SheetColumn {
        Text("Editar nombre", color = MoniColors.fg, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = value, onValueChange = { value = it }, label = { Text("Nombre") },
            singleLine = true, enabled = !state.busy, shape = FieldShape, colors = moniFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        AlertBanner(state.error)
        PrimaryButton(
            if (state.busy) "Guardando…" else "Guardar", onClick = { onSave(value) },
            enabled = !state.busy && value.isNotBlank(), loading = state.busy,
        )
    }
}

@Composable
private fun PasswordContent(state: ProfileState, onSave: (String, () -> Unit) -> Unit) {
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPass by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var touched by rememberSaveable { mutableStateOf(false) }
    val tooShort = pass.length < 6
    val mismatch = pass != confirm
    val fieldError = when {
        !touched -> null
        tooShort -> "Mínimo 6 caracteres"
        mismatch -> "Las contraseñas no coinciden"
        else -> null
    }
    SheetColumn {
        Text("Cambiar contraseña", color = MoniColors.fg, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = pass, onValueChange = { pass = it }, label = { Text("Nueva contraseña") },
            singleLine = true, enabled = !state.busy, shape = FieldShape, colors = moniFieldColors(),
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { showPass = !showPass },
                    modifier = Modifier.focusProperties { canFocus = false },
                ) {
                    LucideIcon(
                        if (showPass) LucideR.drawable.lucide_ic_eye_off else LucideR.drawable.lucide_ic_eye,
                        if (showPass) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = MoniColors.mut,
                        size = 20,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = touched && tooShort, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it }, label = { Text("Confirmar contraseña") },
            singleLine = true, enabled = !state.busy, shape = FieldShape, colors = moniFieldColors(),
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { showConfirm = !showConfirm },
                    modifier = Modifier.focusProperties { canFocus = false },
                ) {
                    LucideIcon(
                        if (showConfirm) LucideR.drawable.lucide_ic_eye_off else LucideR.drawable.lucide_ic_eye,
                        if (showConfirm) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = MoniColors.mut,
                        size = 20,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = touched && !tooShort && mismatch, modifier = Modifier.fillMaxWidth(),
        )
        if (fieldError != null) FieldMessage(fieldError, isError = true)
        AlertBanner(state.error)
        AlertBanner(state.notice, isError = false)
        PrimaryButton(
            if (state.busy) "Guardando…" else "Actualizar contraseña",
            onClick = {
                touched = true
                if (!tooShort && !mismatch) onSave(pass) { pass = ""; confirm = ""; touched = false }
            },
            enabled = !state.busy, loading = state.busy,
        )
    }
}
