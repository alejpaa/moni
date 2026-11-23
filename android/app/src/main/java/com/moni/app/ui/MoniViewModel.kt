package com.moni.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moni.app.data.Transaction
import com.moni.app.data.TransactionRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import com.moni.app.data.supabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val email: String? = null,
    val transactions: List<Transaction> = emptyList(),
    val error: String? = null,
)

class MoniViewModel(
    private val repo: TransactionRepository = TransactionRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _state.update { it.copy(signedIn = true, loading = false, email = repo.currentEmail()) }
                        refresh()
                    }
                    is SessionStatus.NotAuthenticated ->
                        _state.update { UiState(loading = false) }
                    else -> Unit
                }
            }
        }
    }

    fun signIn(email: String, password: String) = run { repo.signIn(email, password) }
    fun signUp(email: String, password: String) = run { repo.signUp(email, password) }
    fun signOut() = run { repo.signOut() }

    fun refresh() = run {
        val items = repo.list()
        _state.update { it.copy(transactions = items) }
    }

    fun add(type: String, amount: Double, category: String, note: String?) = run {
        repo.add(type, amount, category, note)
        _state.update { it.copy(transactions = repo.list()) }
    }

    fun delete(id: String) = run {
        repo.delete(id)
        _state.update { s -> s.copy(transactions = s.transactions.filterNot { it.id == id }) }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Error inesperado") }
            }
        }
    }
}
