package com.moni.app

import android.graphics.Color
import android.os.Bundle
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
import androidx.compose.runtime.key
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
import com.moni.app.ui.LocalRevealEpoch
import com.moni.app.ui.LockScreen
import com.moni.app.ui.LoginScreen
import com.moni.app.ui.MoniViewModel
import com.moni.app.ui.ProfileViewModel
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.MoniTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply FLAG_SECURE synchronously before the first frame when lock or hide-amounts is enabled.
        updateSecureFlag(Prefs.appLockEnabled.value || Prefs.hideAmounts.value)

        // Always light for now: dark icons on transparent system bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MoniTheme {
                val hideAmounts by Prefs.hideAmounts.collectAsStateWithLifecycle()
                val appLockEnabled by Prefs.appLockEnabled.collectAsStateWithLifecycle()
                val revealEpoch by Prefs.revealEpoch.collectAsStateWithLifecycle()
                val vm: MoniViewModel = viewModel()
                val profileVm: ProfileViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(appLockEnabled, hideAmounts) {
                    updateSecureFlag(appLockEnabled || hideAmounts)
                }

                LaunchedEffect(state.loading, state.signedIn) {
                    vm.onAuthStateChanged(state.signedIn, state.loading)
                    if (!state.loading && !state.signedIn) {
                        profileVm.reset()
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                if (!isChangingConfigurations) {
                                    vm.onAppBackgrounded()
                                    Prefs.bumpRevealEpoch()
                                }
                            }
                            Lifecycle.Event.ON_START -> {
                                vm.onAppForegrounded()
                            }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                CompositionLocalProvider(
                    LocalHideAmounts provides hideAmounts,
                    LocalRevealEpoch provides revealEpoch,
                ) {
                    Surface(Modifier.fillMaxSize(), color = MoniColors.bg) {
                        when {
                            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MoniColors.acc)
                            }
                            state.signedIn && appLockEnabled && !vm.isUnlocked ->
                                LockScreen(
                                    onUnlocked = vm::unlock,
                                    onSignOut = vm::signOutAndDisableLock,
                                    signingOut = vm.signingOut,
                                )
                            state.signedIn -> key(state.sessionKey) {
                                AppShell(state, vm, profileVm)
                            }
                            else -> LoginScreen(state.error, state.notice, state.busy, vm::signIn, vm::signUp, vm::clearError)
                        }
                    }
                }
            }
        }
    }

    private fun updateSecureFlag(secure: Boolean) {
        if (secure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
