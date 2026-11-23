package com.moni.app.ui

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.ui.theme.MoniColors

private const val MIN_PASSWORD = 6

private fun emailError(v: String): String? = when {
    v.isBlank() -> "Escribe tu email"
    !Patterns.EMAIL_ADDRESS.matcher(v.trim()).matches() -> "Introduce un email válido"
    else -> null
}

private fun passwordError(v: String): String? = when {
    v.isEmpty() -> "Escribe tu contraseña"
    v.length < MIN_PASSWORD -> "La contraseña debe tener al menos $MIN_PASSWORD caracteres"
    else -> null
}

@Composable
fun LoginScreen(
    error: String?,
    notice: String?,
    busy: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(0) } // 0 = Entrar, 1 = Crear cuenta
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var passwordTouched by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val signUp = mode == 1

    val eErr = if (emailTouched || submitted) emailError(email) else null
    val pErr = if (passwordTouched || submitted) passwordError(password) else null

    fun submit() {
        submitted = true
        if (busy || emailError(email) != null || passwordError(password) != null) return
        focus.clearFocus()
        if (signUp) onSignUp(email.trim(), password) else onSignIn(email.trim(), password)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Entrance(Modifier.fillMaxWidth()) {
            Column {
                Text("MONI", color = MoniColors.mut, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (signUp) "Crea tu cuenta" else "Bienvenido de nuevo",
                    color = MoniColors.fg,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(24.dp))

                SegmentedControl(
                    options = listOf("Entrar", "Crear cuenta"),
                    selected = mode,
                    onSelect = {
                        if (it != mode && !busy) {
                            mode = it
                            submitted = false
                            emailTouched = false
                            passwordTouched = false
                            onClearError()
                        }
                    },
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !busy,
                    isError = eErr != null,
                    shape = FieldShape,
                    colors = moniFieldColors(),
                    leadingIcon = { LucideIcon(LucideR.drawable.lucide_ic_mail, null, tint = MoniColors.mut) },
                    supportingText = eErr?.let { { FieldMessage(it, true) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.EmailAddress }
                        .onFocusChanged { if (!it.isFocused && email.isNotEmpty()) emailTouched = true },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    enabled = !busy,
                    isError = pErr != null,
                    shape = FieldShape,
                    colors = moniFieldColors(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { LucideIcon(LucideR.drawable.lucide_ic_lock, null, tint = MoniColors.mut) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) {
                            LucideIcon(
                                if (showPassword) LucideR.drawable.lucide_ic_eye_off else LucideR.drawable.lucide_ic_eye,
                                if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = MoniColors.mut,
                                size = 20,
                            )
                        }
                    },
                    supportingText = when {
                        pErr != null -> ({ FieldMessage(pErr, true) })
                        signUp -> ({ FieldMessage("Mínimo $MIN_PASSWORD caracteres", false) })
                        else -> null
                    },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = if (signUp) ContentType.NewPassword else ContentType.Password }
                        .onFocusChanged { if (!it.isFocused && password.isNotEmpty()) passwordTouched = true },
                )
                Spacer(Modifier.height(8.dp))

                AlertBanner(error ?: notice, isError = error != null)
                Spacer(Modifier.height(12.dp))

                PrimaryButton(
                    text = when {
                        busy && signUp -> "Creando cuenta…"
                        busy -> "Entrando…"
                        signUp -> "Crear cuenta"
                        else -> "Entrar"
                    },
                    onClick = { submit() },
                    enabled = !busy,
                    loading = busy,
                )
            }
        }
    }
}
