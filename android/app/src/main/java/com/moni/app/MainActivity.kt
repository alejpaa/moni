package com.moni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.moni.app.ui.theme.MoniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoniTheme {
                Surface(Modifier.fillMaxSize()) {
                    val vm: MoniViewModel = viewModel()
                    val state by vm.state.collectAsStateWithLifecycle()
                    when {
                        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        state.signedIn -> HomeScreen(state, vm::add, vm::delete, vm::signOut)
                        else -> LoginScreen(state.error, vm::signIn, vm::signUp)
                    }
                }
            }
        }
    }
}
