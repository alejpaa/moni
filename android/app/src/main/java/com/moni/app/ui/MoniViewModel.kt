package com.moni.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moni.app.data.Account
import com.moni.app.data.Category
import com.moni.app.data.NewReminder
import com.moni.app.data.Reminder
import com.moni.app.data.ReminderRepository
import com.moni.app.data.Transaction
import com.moni.app.data.TransactionRepository
import com.moni.app.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class UiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val email: String? = null,
    val transactions: List<Transaction> = emptyList(),
    /** Rows with status=pending ("por revisar"). */
    val pending: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    /** True once the first transactions fetch finished. */
    val loaded: Boolean = false,
    /** A user-initiated request (sign in/up, add) is in flight. */
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

data class RecurringInput(
    val title: String,
    val frequency: String,
    val dueDate: String,
)

class MoniViewModel(
    private val repo: TransactionRepository = TransactionRepository(),
    private val reminderRepo: ReminderRepository = ReminderRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _state.update { it.copy(signedIn = true, loading = false, email = repo.currentEmail(), notice = null) }
                        refresh()
                    }
                    is SessionStatus.NotAuthenticated ->
                        _state.update { UiState(loading = false) }
                    else -> Unit
                }
            }
        }
    }

    fun signIn(email: String, password: String) = run(busy = true) { repo.signIn(email, password) }

    fun signUp(email: String, password: String) = run(busy = true) {
        repo.signUp(email, password)
        // With email confirmation enabled there is no session yet.
        if (supabase.auth.currentSessionOrNull() == null) {
            _state.update { it.copy(notice = "Te enviamos un correo para confirmar tu cuenta. Después podrás entrar.") }
        }
    }

    fun signOut() = run { repo.signOut() }

    fun refresh() = run {
        val items = repo.list()
        _state.update { it.copy(transactions = items, loaded = true) }
        // Secondary data: a failure here must not hide the main list.
        runCatching { repo.listPending() }.onSuccess { p -> _state.update { it.copy(pending = p) } }
        runCatching { repo.categories() }.onSuccess { c -> _state.update { it.copy(categories = c) } }
        runCatching { repo.accounts() }.onSuccess { a -> _state.update { it.copy(accounts = a) } }
        runCatching { reminderRepo.listActive() }.onSuccess { r -> _state.update { it.copy(reminders = r) } }
    }

    /** [onDone] runs on success so the UI can close the form. */
    fun add(
        type: String,
        amount: Double,
        categoryId: String,
        accountId: String?,
        note: String?,
        recurring: RecurringInput? = null,
        onDone: () -> Unit = {},
    ) = run(busy = true) {
        val reminderId = recurring?.let { r ->
            reminderRepo.create(
                NewReminder(
                    kind = if (type == "income") "receivable" else "payable",
                    title = r.title,
                    amount = amount,
                    dueDate = r.dueDate,
                    frequency = r.frequency,
                    accountId = accountId,
                    categoryId = categoryId,
                    note = note,
                ),
            ).id
        }
        repo.add(type, amount, categoryId, accountId, note, reminderId = reminderId)
        val refreshedReminders = if (recurring != null) runCatching { reminderRepo.listActive() }.getOrNull() else null
        _state.update { s ->
            s.copy(
                transactions = repo.list(),
                reminders = refreshedReminders ?: s.reminders,
                loaded = true,
            )
        }
        onDone()
    }

    /** Confirms a pending row (optionally with a new category/account). */
    fun confirm(id: String, categoryId: String?, accountId: String?) = run {
        repo.review(id, "confirmed", categoryId, accountId)
        _state.update { it.copy(pending = it.pending.filterNot { t -> t.id == id }, transactions = repo.list()) }
    }

    fun ignore(id: String) = run {
        repo.review(id, "ignored")
        _state.update { s -> s.copy(pending = s.pending.filterNot { it.id == id }) }
    }

    fun delete(id: String) = run {
        repo.delete(id)
        _state.update { s -> s.copy(transactions = s.transactions.filterNot { it.id == id }) }
    }

    fun clearError() = _state.update { it.copy(error = null, notice = null) }

    private fun run(busy: Boolean = false, block: suspend () -> Unit) {
        if (busy && _state.value.busy) return
        viewModelScope.launch {
            if (busy) _state.update { it.copy(busy = true, error = null, notice = null) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            } finally {
                if (busy) _state.update { it.copy(busy = false) }
            }
        }
    }
}

/** Maps Supabase / network failures to short Spanish messages. */
fun friendlyError(e: Throwable): String {
    val msg = (e.message ?: "").lowercase()
    return when {
        "invalid login credentials" in msg || "invalid_credentials" in msg ->
            "Email o contraseña incorrectos"
        "already registered" in msg || "user_already_exists" in msg || "already been registered" in msg ->
            "Ya existe una cuenta con este email"
        "weak_password" in msg || "password should be" in msg || "weak password" in msg ->
            "La contraseña es demasiado débil. Usa al menos 6 caracteres"
        "email not confirmed" in msg || "email_not_confirmed" in msg ->
            "Confirma tu email antes de entrar"
        "rate limit" in msg || "over_email_send_rate_limit" in msg || "too many" in msg ->
            "Demasiados intentos. Espera un momento e inténtalo de nuevo"
        "row-level security" in msg || "permission denied" in msg || "42501" in msg ->
            "No tienes permiso para hacer esto"
        "foreign key" in msg || "23503" in msg -> "La categoría o cuenta ya no existe. Actualiza e inténtalo de nuevo"
        "jwt expired" in msg || "jwt" in msg && "invalid" in msg ->
            "Tu sesión expiró. Vuelve a entrar"
        "invalid" in msg && "email" in msg -> "El email no es válido"
        e is IOException || "unable to resolve host" in msg || "timeout" in msg || "timed out" in msg ||
            "failed to connect" in msg || "connect" in msg && "refused" in msg ->
            "Sin conexión. Revisa tu internet e inténtalo de nuevo"
        else -> "Algo salió mal. Inténtalo de nuevo"
    }
}
