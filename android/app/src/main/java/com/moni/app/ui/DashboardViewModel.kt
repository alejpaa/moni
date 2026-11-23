package com.moni.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moni.app.data.AccountRepository
import com.moni.app.data.DashboardRepository
import com.moni.app.data.TxLite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardData(
    val month: List<TxLite> = emptyList(),
    /** Balance per account id (debt for credit cards), from the account_balances view. */
    val balances: Map<String, Double> = emptyMap(),
)

class DashboardViewModel(
    private val repo: DashboardRepository = DashboardRepository(),
    private val accounts: AccountRepository = AccountRepository(),
) : ViewModel() {
    private val _data = MutableStateFlow<DashboardData?>(null)
    /** Null until the first fetch succeeds (callers fall back to UiState.transactions). */
    val data: StateFlow<DashboardData?> = _data.asStateFlow()

    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val month = repo.currentMonth()
                val balances = accounts.balances()
                _data.value = DashboardData(month, balances)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the fallback / last good data; the dashboard is read-only.
            }
        }
    }
}
