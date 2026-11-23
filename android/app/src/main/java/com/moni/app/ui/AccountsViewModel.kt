package com.moni.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moni.app.data.Account
import com.moni.app.data.AccountInput
import com.moni.app.data.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    /** Balance per account id; for credit cards it is the debt. */
    val balances: Map<String, Double> = emptyMap(),
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
)

class AccountsViewModel(
    private val repo: AccountRepository = AccountRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(AccountsUiState())
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val accounts = repo.list(includeArchived = true)
                val balances = repo.balances()
                _state.update { it.copy(accounts = accounts, balances = balances, loaded = true, loading = false, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loaded = true, error = friendlyError(e)) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** [id] == null creates. [onDone] runs after success. */
    fun save(id: String?, input: AccountInput, onDone: () -> Unit) = run(onDone) {
        if (id == null) repo.create(input) else repo.update(id, input)
    }

    fun setArchived(id: String, archived: Boolean, onDone: () -> Unit) = run(onDone) {
        repo.setArchived(id, archived)
    }

    private fun run(onDone: () -> Unit, block: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                block()
                _state.update { it.copy(busy = false) }
                reload()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = accountError(e)) }
            }
        }
    }

    private fun accountError(e: Throwable): String {
        val msg = (e.message ?: "").lowercase()
        return if ("23505" in msg || "duplicate key" in msg || "accounts_workspace" in msg) {
            "Ya tienes una cuenta con ese nombre"
        } else friendlyError(e)
    }
}
