package com.moni.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moni.app.data.Account
import com.moni.app.data.AccountInput
import com.moni.app.data.AccountRepository
import com.moni.app.data.Category
import com.moni.app.data.CategoryRepository
import com.moni.app.data.DashboardRepository
import com.moni.app.data.NewReminder
import com.moni.app.data.Reminder
import com.moni.app.data.ReminderRepository
import com.moni.app.data.Transaction
import com.moni.app.data.TransactionRepository
import com.moni.app.data.TxLite
import com.moni.app.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class UiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val sessionKey: Int = 0,
    val email: String? = null,
    val transactions: List<Transaction> = emptyList(),
    /** Rows with status=pending ("por revisar"). */
    val pending: List<Transaction> = emptyList(),
    /** Active and archived categories. */
    val allCategories: List<Category> = emptyList(),
    /** Active and archived accounts. */
    val allAccounts: List<Account> = emptyList(),
    /** Balance per account id (debt owed for credit cards), from account_balances. */
    val balances: Map<String, Double> = emptyMap(),
    /** Confirmed transactions for the current month (Dashboard aggregates). */
    val monthTransactions: List<TxLite> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    /** True once the first transactions fetch finished. */
    val loaded: Boolean = false,
    val monthLoaded: Boolean = false,
    val accountsLoaded: Boolean = false,
    val balancesLoaded: Boolean = false,
    /** A user-initiated request (sign in/up, add, account/category edit) is in flight. */
    val busy: Boolean = false,
    val error: String? = null,
    val sheetError: String? = null,
    val notice: String? = null,
) {
    val categories: List<Category> get() = allCategories.filter { !it.archived }
    val accounts: List<Account> get() = allAccounts.filter { !it.archived }
}

data class RecurringInput(
    val title: String,
    val frequency: String,
    val dueDate: String,
)

class MoniViewModel(
    private val repo: TransactionRepository = TransactionRepository(),
    private val accountRepo: AccountRepository = AccountRepository(),
    private val categoryRepo: CategoryRepository = CategoryRepository(),
    private val dashboardRepo: DashboardRepository = DashboardRepository(),
    private val reminderRepo: ReminderRepository = ReminderRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadedUserId: String? = null
    private var hasLoadedForSession = false

    // Session-scoped UI animation trackers so tab switches never replay entrances or count-ups.
    private val enteredTabs = mutableSetOf<String>()
    private var heroAnimated = false
    private var dashboardBarsAnimated = false
    internal val rowTracker = RowTracker()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = status.session.user?.id ?: repo.currentUserId()
                        val email = status.session.user?.email ?: repo.currentEmail()
                        val userChanged = !hasLoadedForSession || (userId != null && userId != loadedUserId)
                        _state.update { s ->
                            s.copy(
                                signedIn = true,
                                loading = false,
                                email = email,
                                notice = null,
                                sessionKey = if (userChanged) s.sessionKey + 1 else s.sessionKey,
                            )
                        }
                        if (userChanged) {
                            hasLoadedForSession = true
                            loadedUserId = userId
                            refresh()
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        hasLoadedForSession = false
                        loadedUserId = null
                        enteredTabs.clear()
                        heroAnimated = false
                        dashboardBarsAnimated = false
                        rowTracker.reset()
                        _state.value = UiState(loading = false)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun shouldAnimateEntrance(tabKey: String): Boolean = tabKey !in enteredTabs
    fun markTabEntered(tabKey: String) {
        enteredTabs += tabKey
    }

    fun shouldAnimateHero(): Boolean = !heroAnimated
    fun markHeroAnimated() {
        heroAnimated = true
    }

    fun shouldAnimateDashboardBars(): Boolean = !dashboardBarsAnimated
    fun markDashboardBarsAnimated() {
        dashboardBarsAnimated = true
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

    /** Loads all shared store slices in parallel; secondary failures must not hide the main list. */
    fun refresh() = run {
        coroutineScope {
            val txDef = async { repo.list() }
            val pendingDef = async { safe { repo.listPending() } }
            val catDef = async { safe { categoryRepo.listAll() } }
            val accDef = async { safe { accountRepo.list(includeArchived = true) } }
            val monthDef = async { safe { dashboardRepo.currentMonth() } }
            val balDef = async { safe { accountRepo.balances() } }
            val remDef = async { safe { reminderRepo.listActive() } }

            val items = txDef.await()
            val pending = pendingDef.await()
            val categories = catDef.await()
            val accounts = accDef.await()
            val month = monthDef.await()
            val balances = balDef.await()
            val reminders = remDef.await()

            _state.update { s ->
                s.copy(
                    transactions = items,
                    loaded = true,
                    pending = pending ?: s.pending,
                    allCategories = categories ?: s.allCategories,
                    allAccounts = accounts ?: s.allAccounts,
                    accountsLoaded = accounts != null || s.accountsLoaded,
                    monthTransactions = month ?: s.monthTransactions,
                    monthLoaded = month != null || s.monthLoaded,
                    balances = balances ?: s.balances,
                    balancesLoaded = balances != null || s.balancesLoaded,
                    reminders = reminders ?: s.reminders,
                )
            }
        }
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
    ) = run(busy = true, sheet = true) {
        val createdReminder = recurring?.let { r ->
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
            )
        }
        val createdTx = repo.add(type, amount, categoryId, accountId, note, reminderId = createdReminder?.id)
        val (balances, month) = refreshBalancesAndMonth()
        _state.update { s ->
            s.copy(
                transactions = (listOf(createdTx) + s.transactions).sortedByDescending { it.occurredAt }.take(100),
                reminders = if (createdReminder != null) (s.reminders + createdReminder).sortedBy { it.dueDate } else s.reminders,
                balances = balances ?: s.balances,
                balancesLoaded = balances != null || s.balancesLoaded,
                monthTransactions = month ?: (s.monthTransactions + TxLite(createdTx.type, createdTx.amount, createdTx.categoryId, createdTx.accountId, createdTx.occurredAt)),
                monthLoaded = true,
                loaded = true,
            )
        }
        onDone()
    }

    /** Confirms a pending row (optionally with a new category/account). */
    fun confirm(id: String, categoryId: String?, accountId: String?) = run(sheet = true) {
        val confirmed = repo.review(id, "confirmed", categoryId, accountId)
        val (balances, month) = refreshBalancesAndMonth()
        _state.update { s ->
            s.copy(
                pending = s.pending.filterNot { it.id == id },
                transactions = (listOf(confirmed) + s.transactions.filterNot { it.id == confirmed.id })
                    .sortedByDescending { it.occurredAt }
                    .take(100),
                balances = balances ?: s.balances,
                balancesLoaded = balances != null || s.balancesLoaded,
                monthTransactions = month ?: s.monthTransactions,
                monthLoaded = month != null || s.monthLoaded,
            )
        }
    }

    fun ignore(id: String) = run(sheet = true) {
        repo.review(id, "ignored")
        _state.update { s -> s.copy(pending = s.pending.filterNot { it.id == id }) }
    }

    fun delete(id: String) = run {
        repo.delete(id)
        val (balances, month) = refreshBalancesAndMonth()
        _state.update { s ->
            s.copy(
                transactions = s.transactions.filterNot { it.id == id },
                balances = balances ?: s.balances,
                balancesLoaded = balances != null || s.balancesLoaded,
                monthTransactions = month ?: s.monthTransactions,
                monthLoaded = month != null || s.monthLoaded,
            )
        }
    }

    /** [id] == null creates. [onDone] runs after success. */
    fun saveAccount(id: String?, input: AccountInput, onDone: () -> Unit) =
        run(busy = true, sheet = true, mapError = ::accountError) {
            if (id == null) accountRepo.create(input) else accountRepo.update(id, input)
            coroutineScope {
                val accDef = async { accountRepo.list(includeArchived = true) }
                val balDef = async { safe { accountRepo.balances() } }
                val accounts = accDef.await()
                val balances = balDef.await()
                _state.update { s ->
                    s.copy(
                        allAccounts = accounts,
                        accountsLoaded = true,
                        balances = balances ?: s.balances,
                        balancesLoaded = balances != null || s.balancesLoaded,
                    )
                }
            }
            onDone()
        }

    fun setAccountArchived(id: String, archived: Boolean, fromSheet: Boolean = false, onDone: () -> Unit = {}) =
        run(busy = true, sheet = fromSheet, mapError = ::accountError) {
            accountRepo.setArchived(id, archived)
            _state.update { s ->
                s.copy(allAccounts = s.allAccounts.map { if (it.id == id) it.copy(archived = archived) else it })
            }
            onDone()
        }

    fun createCategory(name: String, kind: String, icon: String, onDone: () -> Unit) =
        run(busy = true, sheet = true, mapError = ::categoryError) {
            categoryRepo.create(name.trim(), kind, icon)
            val categories = categoryRepo.listAll()
            _state.update { it.copy(allCategories = categories) }
            onDone()
        }

    fun updateCategory(id: String, name: String, icon: String, onDone: () -> Unit) =
        run(busy = true, sheet = true, mapError = ::categoryError) {
            categoryRepo.update(id, name.trim(), icon)
            val categories = categoryRepo.listAll()
            _state.update { it.copy(allCategories = categories) }
            onDone()
        }

    fun setCategoryArchived(id: String, archived: Boolean, onDone: () -> Unit = {}) =
        run(busy = true, sheet = true, mapError = ::categoryError) {
            categoryRepo.setArchived(id, archived)
            _state.update { s ->
                s.copy(allCategories = s.allCategories.map { if (it.id == id) it.copy(archived = archived) else it })
            }
            onDone()
        }

    fun clearError() = _state.update { it.copy(error = null, sheetError = null, notice = null) }

    private suspend fun refreshBalancesAndMonth(): Pair<Map<String, Double>?, List<TxLite>?> = coroutineScope {
        val balDef = async { safe { accountRepo.balances() } }
        val monthDef = async { safe { dashboardRepo.currentMonth() } }
        balDef.await() to monthDef.await()
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun run(
        busy: Boolean = false,
        sheet: Boolean = false,
        mapError: (Throwable) -> String = ::friendlyError,
        block: suspend () -> Unit,
    ) {
        if (busy && _state.value.busy) return
        viewModelScope.launch {
            if (busy) {
                _state.update {
                    it.copy(
                        busy = true,
                        error = if (sheet) it.error else null,
                        sheetError = if (sheet) null else it.sheetError,
                        notice = null,
                    )
                }
            }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = mapError(e)
                _state.update { if (sheet) it.copy(sheetError = msg) else it.copy(error = msg) }
            } finally {
                if (busy) _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun accountError(e: Throwable): String {
        val msg = (e.message ?: "").lowercase()
        return if ("23505" in msg || "duplicate key" in msg || "accounts_workspace" in msg) {
            "Ya tienes una cuenta con ese nombre"
        } else friendlyError(e)
    }

    private fun categoryError(e: Throwable): String {
        val msg = (e.message ?: "").lowercase()
        return if ("23505" in msg || "duplicate key" in msg) {
            "Ya existe una categoría con ese nombre"
        } else friendlyError(e)
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
