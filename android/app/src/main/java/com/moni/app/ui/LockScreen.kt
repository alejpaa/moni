package com.moni.app.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composables.icons.lucide.R as LucideR
import com.moni.app.ui.theme.MoniColors

internal const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

internal data class BiometricStatus(val available: Boolean, val reason: String?)

internal fun biometricStatus(context: Context): BiometricStatus {
    val manager = BiometricManager.from(context)
    return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus(true, null)
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            BiometricStatus(false, "Configura una huella o bloqueo de pantalla en Ajustes para activarlo")
        else ->
            BiometricStatus(false, "Tu dispositivo no tiene biometría ni bloqueo de pantalla disponible")
    }
}

internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

internal fun createBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit,
): BiometricPrompt = BiometricPrompt(
    activity,
    ContextCompat.getMainExecutor(activity),
    object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_CANCELED
            onError(if (cancelled) null else errString.toString())
        }
    },
)

internal fun authenticateBiometric(
    prompt: BiometricPrompt,
    title: String,
    subtitle: String,
) {
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}

internal fun promptBiometric(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit,
) {
    val prompt = createBiometricPrompt(activity, onSuccess, onError)
    authenticateBiometric(prompt, title, subtitle)
}

@Composable
internal fun LockScreen(
    onUnlocked: () -> Unit,
    onSignOut: () -> Unit,
    signingOut: Boolean = false,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var bioStatus by remember { mutableStateOf(biometricStatus(context)) }
    var prompting by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    // Register the callback on each new FragmentActivity after rotation so in-flight prompts still deliver results.
    val prompt = remember(activity) {
        activity?.let {
            createBiometricPrompt(
                activity = it,
                onSuccess = {
                    prompting = false
                    currentOnUnlocked()
                },
                onError = { msg ->
                    prompting = false
                    error = msg
                },
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bioStatus = biometricStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun launchPrompt() {
        if (prompting || prompt == null || !bioStatus.available) return
        prompting = true
        error = null
        authenticateBiometric(
            prompt = prompt,
            title = "Desbloquear moni",
            subtitle = "Confirma tu identidad para continuar",
        )
    }

    LaunchedEffect(bioStatus.available) {
        if (bioStatus.available && !prompting) {
            launchPrompt()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoniMascot(96.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            "moni bloqueado",
            color = MoniColors.fg,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (bioStatus.available) {
                "Usa tu huella o bloqueo de pantalla para ver tus finanzas."
            } else {
                bioStatus.reason ?: "No hay método de desbloqueo disponible en este dispositivo."
            },
            color = MoniColors.mut,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        AlertBanner(error)
        if (error != null) Spacer(Modifier.height(16.dp))
        if (bioStatus.available) {
            PrimaryButton(
                text = "Desbloquear",
                onClick = { launchPrompt() },
                enabled = !prompting && !signingOut,
                icon = LucideR.drawable.lucide_ic_fingerprint_pattern,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onSignOut,
                enabled = !signingOut,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    if (signingOut) "Cerrando sesión…" else "Cerrar sesión",
                    color = MoniColors.mut,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            PrimaryButton(
                text = if (signingOut) "Cerrando sesión…" else "Cerrar sesión",
                onClick = onSignOut,
                enabled = !signingOut,
                loading = signingOut,
                icon = LucideR.drawable.lucide_ic_log_out,
            )
        }
    }
}
