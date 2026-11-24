package com.moni.app

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moni.app.data.Prefs
import com.moni.app.ui.AppShell
import com.moni.app.ui.LocalHideAmounts
import com.moni.app.ui.LockScreen
import com.moni.app.ui.LoginScreen
import com.moni.app.ui.MoniViewModel
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.MoniTheme

private const val RELOCK_BACKGROUND_MS = 30_000L

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light for now: dark icons on transparent system bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MoniTheme {
                val hideAmounts by Prefs.hideAmounts.collectAsStateWithLifecycle()
                val appLockEnabled by Prefs.appLockEnabled.collectAsStateWithLifecycle()
                var isUnlocked by rememberSaveable { mutableStateOf(!Prefs.appLockEnabled.value) }
                var backgroundedAtMs by rememberSaveable { mutableStateOf<Long?>(null) }

                LaunchedEffect(appLockEnabled) {
                    if (appLockEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                if (!isChangingConfigurations) {
                                    backgroundedAtMs = SystemClock.elapsedRealtime()
                                }
                            }
                            Lifecycle.Event.ON_START -> {
                                val leftAt = backgroundedAtMs
                                backgroundedAtMs = null
                                if (leftAt != null &&
                                    Prefs.appLockEnabled.value &&
                                    (SystemClock.elapsedRealtime() - leftAt) > RELOCK_BACKGROUND_MS
                                ) {
                                    isUnlocked = false
                                }
                            }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                CompositionLocalProvider(LocalHideAmounts provides hideAmounts) {
                    Surface(Modifier.fillMaxSize(), color = MoniColors.bg) {
                        val vm: MoniViewModel = viewModel()
                        val state by vm.state.collectAsStateWithLifecycle()
                        LaunchedEffect(state.loading, state.signedIn) {
                            if (!state.loading && !state.signedIn) {
                                isUnlocked = true
                            }
                        }
                        when {
                            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MoniColors.acc)
                            }
                            state.signedIn && appLockEnabled && !isUnlocked ->
                                LockScreen(onUnlocked = { isUnlocked = true })
                            state.signedIn -> AppShell(state, vm)
                            else -> LoginScreen(state.error, state.notice, state.busy, vm::signIn, vm::signUp, vm::clearError)
                        }
                    }
                }
            }
        }
    }
}
