package com.moni.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moni.app.ui.HomeScreen
import com.moni.app.ui.LoginScreen
import com.moni.app.ui.MoniViewModel
import com.moni.app.ui.theme.MoniColors
import com.moni.app.ui.theme.MoniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light for now: dark icons on transparent system bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MoniTheme {
                Surface(Modifier.fillMaxSize(), color = MoniColors.bg) {
                    val vm: MoniViewModel = viewModel()
                    val state by vm.state.collectAsStateWithLifecycle()
                    when {
                        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MoniColors.acc)
                        }
                        state.signedIn -> HomeScreen(state, vm::add, vm::delete, vm::signOut, vm::clearError)
                        else -> LoginScreen(state.error, state.notice, state.busy, vm::signIn, vm::signUp, vm::clearError)
                    }
                }
            }
        }
    }
}
