package com.moni.app.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moni.app.data.Account
import com.moni.app.data.AccountBalance
import com.moni.app.data.AccountInput
import com.moni.app.data.AccountRepository
import com.moni.app.data.Category
import com.moni.app.data.CategoryRepository
import com.moni.app.data.DashboardRepository
import com.moni.app.data.NewReminder
import com.moni.app.data.Prefs
import com.moni.app.data.Reminder
import com.moni.app.data.ReminderRepository
import com.moni.app.data.Transaction
import com.moni.app.data.TransactionRepository
import com.moni.app.data.TxLite
import com.moni.app.data.WorkspaceRepository
import com.moni.app.data.isInCurrentLimaMonth
import com.moni.app.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

private const val RELOCK_BACKGROUND_MS = 30_000L

data class UiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val sessionKey: Int = 0,
    val email: String? = null,
    val workspaceId: String? = null,
    val workspaceRole: String? = null,
    val transactions: List<Transaction> = emptyList(),
    val hasMoreTransactions: Boolean = false,
    val loadingMoreTransactions: Boolean = false,
    /** Rows with status=pending ("por revisar"). */
    val pending: List<Transaction> = emptyList(),
    /** Active and archived categories. */
    val allCategories: List<Category> = emptyList(),
    val categoriesLoaded: Boolean = false,
    val categoriesError: String? = null,
    /** Active and archived accounts. */
    val allAccounts: List<Account> = emptyList(),
    /** Balance and credit info per account id from account_balances. */
    val balances: Map<String, AccountBalance> = emptyMap(),
    /** Confirmed transactions for the current month (Dashboard aggregates). */
    val monthTransactions: List<TxLite> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    /** True once the first transactions fetch finished. */
    val loaded: Boolean = false,
    val loadFailed: Boolean = false,
    val monthLoaded: Boolean = false,
    val accountsLoaded: Boolean = false,
    val balancesLoaded: Boolean = false,
    val sessionRefreshFailed: Boolean = false,
    /** A user-initiated request (sign in/up, add, account/category edit) is in flight. */
    val busy: Boolean = false,
    val deletingIds: Set<String> = emptySet(),
    val reviewWorkingIds: Set<String> = emptySet(),
    val error: String? = null,
    val sheetError: String? = null,
    val notice: String? = null,
) {
    val categories: List<Category> get() = allCategories.filter { !it.archived }
    val accounts: List<Account> get() = allAccounts.filter { !it.archived }
    val isViewer: Boolean get() = workspaceRole == "viewer"
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
    private val workspaceRepo: WorkspaceRepository = WorkspaceRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /**
     * Kept in ViewModel (NOT SavedStateHandle) so it survives rotation (config changes)
     * but always resets to locked after process death.
     */
    var isUnlocked by mutableStateOf(!Prefs.appLockEnabled.value)
        private set

    var signingOut by mutableStateOf(false)
        private set

    private var loadedUserId: String? = null
    private var hasLoadedForSession = false
    @Volatile
    private var sessionSeq = 0
    private var sessionJob: CompletableJob = SupervisorJob(viewModelScope.coroutineContext[Job])
    private val sessionScope: CoroutineScope
        get() = CoroutineScope(viewModelScope.coroutineContext + sessionJob)
    private var refreshJob: Job? = null
    private val storeMutex = Mutex()
    @Volatile
    private var disableLockOnSignOut = false

    // Session-scoped UI animation trackers so tab switches never replay entrances or count-ups.
    private val enteredTabs = mutableSetOf<String>()
    private var heroAnimated = false
    private var dashboardBarsAnimated = false
    internal val rowTracker = RowTracker()

    private fun resetSessionJobs(): Int {
        refreshJob?.cancel()
        refreshJob = null
        sessionJob.cancel()
        sessionJob = SupervisorJob(viewModelScope.coroutineContext[Job])
        return ++sessionSeq
    }

    private inline fun updateIfSession(seq: Int, transform: (UiState) -> UiState) {
        _state.update { s -> if (sessionSeq == seq) transform(s) else s }
    }

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = status.session.user?.id ?: repo.currentUserId()
                        val email = status.session.user?.email ?: repo.currentEmail()
                        val userChanged = !hasLoadedForSession || (userId != null && userId != loadedUserId)
                        val nextSessionKey = if (userChanged) resetSessionJobs() else _state.value.sessionKey
                        _state.update { s ->
                            if (userChanged) {
                                UiState(
                                    loading = false,
                                    signedIn = true,
                                    sessionKey = nextSessionKey,
                                    email = email,
                                )
                            } else {
                                s.copy(
                                    signedIn = true,
                                    loading = false,
                                    email = email,
                                    sessionRefreshFailed = false,
                                    notice = null,
                                )
                            }
                        }
                        if (userChanged) {
                            hasLoadedForSession = true
                            loadedUserId = userId
                            enteredTabs.clear()
                            heroAnimated = false
                            dashboardBarsAnimated = false
                            rowTracker.reset()
                            refresh()
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        val nextSessionKey = resetSessionJobs()
                        hasLoadedForSession = false
                        loadedUserId = null
                        enteredTabs.clear()
                        heroAnimated = false
                        dashboardBarsAnimated = false
                        rowTracker.reset()
                        isUnlocked = true
                        Prefs.clearBackgroundedAtMs()
                        _state.value = UiState(loading = false, sessionKey = nextSessionKey)
                        if (disableLockOnSignOut) {
                            disableLockOnSignOut = false
                            Prefs.setAppLockEnabled(false)
                        }
                    }
                    is SessionStatus.RefreshFailure -> {
                        val hasCachedSession = _state.value.signedIn || supabase.auth.currentSessionOrNull() != null
                        _state.update { s ->
                            s.copy(
                                loading = false,
                                signedIn = hasCachedSession,
                                sessionRefreshFailed = true,
                                error = "No se pudo verificar tu sesión. Revisa tu conexión.",
                            )
                        }
                    }
                    is SessionStatus.Initializing -> {
                        _state.update { it.copy(loading = true) }
                    }
                }
            }
        }
    }

    fun unlock() {
        if (signingOut) return
        isUnlocked = true
    }

    fun onAppBackgrounded() {
        if (Prefs.appLockEnabled.value) {
            Prefs.setBackgroundedAtMsSync(SystemClock.elapsedRealtime())
        }
    }

    fun onAppForegrounded() {
        val leftAt = Prefs.consumeBackgroundedAtMs() ?: return
        if (Prefs.appLockEnabled.value && (SystemClock.elapsedRealtime() - leftAt) > RELOCK_BACKGROUND_MS) {
            isUnlocked = false
        }
    }

    fun onAuthStateChanged(signedIn: Boolean, loading: Boolean) {
        if (!loading && !signedIn) {
            isUnlocked = true
        }
    }

    fun signOutAndDisableLock() = performSignOut(disableLock = true)

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

    fun signIn(email: String, password: String) = run(busy = true, sessionScoped = false) { _ -> repo.signIn(email, password) }

    fun signUp(email: String, password: String) = run(busy = true, sessionScoped = false) { _ ->
        repo.signUp(email, password)
        if (supabase.auth.currentSessionOrNull() == null) {
            _state.update { it.copy(notice = "Te enviamos un correo para confirmar tu cuenta. Después podrás entrar.") }
        }
    }

    fun signOut() = performSignOut(disableLock = false)

    private fun performSignOut(disableLock: Boolean) {
        if (signingOut) return
        signingOut = true
        if (disableLock) {
            disableLockOnSignOut = true
        }
        viewModelScope.launch {
            try {
                repo.signOut()
                if (disableLockOnSignOut && !_state.value.signedIn) {
                    disableLockOnSignOut = false
                    Prefs.setAppLockEnabled(false)
                    Prefs.clearBackgroundedAtMs()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (disableLock) disableLockOnSignOut = false
                _state.update { it.copy(error = friendlyError(e)) }
            } finally {
                signingOut = false
            }
        }
    }

    fun retrySessionOrRefresh() {
        val seq = sessionSeq
        val wasRefreshFailed = _state.value.sessionRefreshFailed
        sessionScope.launch {
            updateIfSession(seq) { it.copy(error = null, sessionRefreshFailed = false) }
            try {
                if (wasRefreshFailed || supabase.auth.currentSessionOrNull() != null) {
                    supabase.auth.refreshCurrentSession()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateIfSession(seq) { it.copy(error = friendlyError(e), sessionRefreshFailed = true) }
                return@launch
            }
            if (sessionSeq == seq) refresh()
        }
    }

    private suspend fun ensureWorkspace(seq: Int): String {
        if (sessionSeq != seq) throw CancellationException("Session changed")
        _state.value.workspaceId?.let { return it }
        val ws = workspaceRepo.resolveActiveWorkspace()
        if (sessionSeq != seq) throw CancellationException("Session changed")
        updateIfSession(seq) { it.copy(workspaceId = ws.workspaceId, workspaceRole = ws.role) }
        return ws.workspaceId
    }

    /** Loads all shared store slices in parallel, serialized against concurrent refreshes/mutations. */
    fun refresh() {
        val prev = refreshJob
        val seq = sessionSeq
        refreshJob = sessionScope.launch {
            prev?.cancelAndJoin()
            if (sessionSeq != seq) return@launch
            updateIfSession(seq) { it.copy(error = null, loadFailed = false, sessionRefreshFailed = false) }
            try {
                storeMutex.withLock {
                    val wsId = ensureWorkspace(seq)
                    coroutineScope {
                        val txDef = async { repo.listPage(wsId, offset = 0L) }
                        val pendingDef = async { safeResult { repo.listPending(wsId) } }
                        val catDef = async { safeResult { categoryRepo.listAll(wsId) } }
                        val accDef = async { safeResult { accountRepo.list(wsId, includeArchived = true) } }
                        val monthDef = async { safeResult { dashboardRepo.currentMonth(wsId) } }
                        val balDef = async { safeResult { accountRepo.balances(wsId) } }
                        val remDef = async { safeResult { reminderRepo.listActive(wsId) } }

                        val items = txDef.await()
                        val pendingRes = pendingDef.await()
                        val catRes = catDef.await()
                        val accRes = accDef.await()
                        val monthRes = monthDef.await()
                        val balRes = balDef.await()
                        val remRes = remDef.await()

                        val pending = pendingRes.getOrNull()
                        val categories = catRes.getOrNull()
                        val accounts = accRes.getOrNull()
                        val month = monthRes.getOrNull()
                        val balances = balRes.getOrNull()
                        val reminders = remRes.getOrNull()

                        val firstSecondaryError = listOfNotNull(
                            pendingRes.exceptionOrNull(),
                            accRes.exceptionOrNull(),
                            monthRes.exceptionOrNull(),
                            balRes.exceptionOrNull(),
                            remRes.exceptionOrNull(),
                        ).firstOrNull()?.let(::friendlyError)

                        if (sessionSeq != seq) return@coroutineScope
                        rowTracker.update(items.map { it.id }, loaded = true)

                        updateIfSession(seq) { s ->
                            s.copy(
                                transactions = items,
                                hasMoreTransactions = items.size >= TransactionRepository.PAGE_SIZE.toInt(),
                                loaded = true,
                                loadFailed = false,
                                pending = pending ?: s.pending,
                                allCategories = categories ?: s.allCategories,
                                categoriesLoaded = categories != null || s.categoriesLoaded,
                                categoriesError = if (categories != null) null else catRes.exceptionOrNull()?.let(::friendlyError) ?: s.categoriesError,
                                allAccounts = accounts ?: s.allAccounts,
                                accountsLoaded = accounts != null || s.accountsLoaded,
                                monthTransactions = month ?: s.monthTransactions,
                                monthLoaded = month != null || s.monthLoaded,
                                balances = balances ?: s.balances,
                                balancesLoaded = balances != null || s.balancesLoaded,
                                reminders = reminders ?: s.reminders,
                                error = firstSecondaryError,
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateIfSession(seq) { s ->
                    s.copy(
                        loadFailed = !s.loaded,
                        error = friendlyError(e),
                    )
                }
            }
        }
    }

    fun loadMoreTransactions() {
        val current = _state.value
        if (current.loadingMoreTransactions || !current.hasMoreTransactions) return
        val seq = sessionSeq
        sessionScope.launch {
            updateIfSession(seq) { it.copy(loadingMoreTransactions = true, error = null) }
            try {
                storeMutex.withLock {
                    val wsId = ensureWorkspace(seq)
                    val offset = _state.value.transactions.size.toLong()
                    val more = repo.listPage(wsId, offset = offset)
                    if (sessionSeq != seq) return@withLock
                    val existingIds = _state.value.transactions.asSequence().map { it.id }.toHashSet()
                    val fresh = more.filterNot { it.id in existingIds }
                    rowTracker.markBaseline(fresh.map { it.id })
                    updateIfSession(seq) { s ->
                        s.copy(
                            transactions = s.transactions + fresh,
                            hasMoreTransactions = more.size >= TransactionRepository.PAGE_SIZE.toInt(),
                            loadingMoreTransactions = false,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateIfSession(seq) { it.copy(loadingMoreTransactions = false, error = friendlyError(e)) }
            } finally {
                updateIfSession(seq) { it.copy(loadingMoreTransactions = false) }
            }
        }
    }

    fun retryCategories() {
        val seq = sessionSeq
        sessionScope.launch {
            updateIfSession(seq) { it.copy(categoriesError = null) }
            try {
                val wsId = ensureWorkspace(seq)
                val categories = categoryRepo.listAll(wsId)
                updateIfSession(seq) {
                    it.copy(
                        allCategories = categories,
                        categoriesLoaded = true,
                        categoriesError = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateIfSession(seq) { it.copy(categoriesError = friendlyError(e)) }
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
    ) = run(busy = true, sheet = true) { seq ->
        if (_state.value.isViewer) return@run
        refreshJob?.cancelAndJoin()
        storeMutex.withLock {
            val wsId = ensureWorkspace(seq)
            val createdReminder = recurring?.let { r ->
                reminderRepo.create(
                    NewReminder(
                        workspaceId = wsId,
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
            val createdTx = try {
                repo.add(wsId, type, amount, categoryId, accountId, note, reminderId = createdReminder?.id)
            } catch (e: CancellationException) {
                if (createdReminder != null) {
                    withContext(NonCancellable) { safeResult { reminderRepo.delete(wsId, createdReminder.id) } }
                }
                throw e
            } catch (e: Exception) {
                if (createdReminder != null) {
                    withContext(NonCancellable) { safeResult { reminderRepo.delete(wsId, createdReminder.id) } }
                }
                throw e
            }
            val (balances, month) = refreshBalancesAndMonth(wsId)
            if (sessionSeq != seq) return@run
            val updatedTx = (listOf(createdTx) + _state.value.transactions).sortedByDescending { it.occurredAt }
            rowTracker.update(updatedTx.map { it.id }, loaded = true)
            updateIfSession(seq) { s ->
                val fallbackMonth = if (isInCurrentLimaMonth(createdTx.occurredAt)) {
                    s.monthTransactions + TxLite(createdTx.type, createdTx.amount, createdTx.categoryId, createdTx.occurredAt)
                } else {
                    s.monthTransactions
                }
                s.copy(
                    transactions = updatedTx,
                    reminders = if (createdReminder != null) (s.reminders + createdReminder).sortedBy { it.dueDate } else s.reminders,
                    balances = balances ?: s.balances,
                    balancesLoaded = balances != null || s.balancesLoaded,
                    monthTransactions = month ?: fallbackMonth,
                    monthLoaded = true,
                    loaded = true,
                )
            }
        }
        if (sessionSeq == seq) onDone()
    }

    /** Confirms a pending row (optionally with a new category/account). */
    fun confirm(id: String, categoryId: String?, accountId: String?) {
        if (_state.value.isViewer || id in _state.value.reviewWorkingIds) return
        val seq = sessionSeq
        sessionScope.launch {
            updateIfSession(seq) { it.copy(reviewWorkingIds = it.reviewWorkingIds + id, sheetError = null) }
            try {
                refreshJob?.cancelAndJoin()
                storeMutex.withLock {
                    val wsId = ensureWorkspace(seq)
                    val confirmed = repo.review(wsId, id, "confirmed", categoryId, accountId)
                    val (balances, month) = refreshBalancesAndMonth(wsId)
                    if (sessionSeq != seq) return@withLock
                    val updatedTx = (listOf(confirmed) + _state.value.transactions.filterNot { it.id == confirmed.id })
                        .sortedByDescending { it.occurredAt }
                    rowTracker.update(updatedTx.map { it.id }, loaded = true)
                    updateIfSession(seq) { s ->
                        s.copy(
                            pending = s.pending.filterNot { it.id == id },
                            transactions = updatedTx,
                            balances = balances ?: s.balances,
                            balancesLoaded = balances != null || s.balancesLoaded,
                            monthTransactions = month ?: s.monthTransactions,
                            monthLoaded = month != null || s.monthLoaded,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateIfSession(seq) { it.copy(sheetError = friendlyError(e)) }
            } finally {
                updateIfSession(seq) { it.copy(reviewWorkingIds = it.reviewWorkingIds - id) }
            }
        }
    }

    fun ignore(id: String) {
        if (_state.value.isViewer || id in _state.value.reviewWorkingIds) return
        val seq = sessionSeq
        sessionScope.launch {
            updateIfSession(seq) { it.copy(reviewWorkingIds = it.reviewWorkingIds + id, sheetError = null) }
            try {
                refreshJob?.cancelAndJoin()
                storeMutex.withLock {
                    val wsId = ensureWorkspace(seq)
                    repo.review(wsId, id, "ignored")
                    updateIfSession(seq) { s -> s.copy(pending = s.pending.filterNot { it.id == id }) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateIfSession(seq) { it.copy(sheetError = friendlyError(e)) }
            } finally {
                updateIfSession(seq) { it.copy(reviewWorkingIds = it.reviewWorkingIds - id) }
            }
        }
    }

    fun delete(id: String) {
        if (_state.value.isViewer || id in _state.value.deletingIds) return
        val seq = sessionSeq
        sessionScope.launch {
            updateIfSession(seq) { it.copy(deletingIds = it.deletingIds + id, error = null) }
            try {
                refreshJob?.cancelAndJoin()
                storeMutex.withLock {
                    val wsId = ensureWorkspace(seq)
                    val existing = _state.value.transactions.find { it.id == id }
                    if (existing?.source == "installment") {
                        repo.review(wsId, id, "ignored")
                    } else {
                        repo.delete(wsId, id)
                    }
                    val (balances, month) = refreshBalancesAndMonth(wsId)
                    updateIfSession(seq) { s ->
                        s.copy(
                            transactions = s.transactions.filterNot { it.id == id },
                            balances = balances ?: s.balances,
                            balancesLoaded = balances != null || s.balancesLoaded,
                            monthTransactions = month ?: s.monthTransactions,
                            monthLoaded = month != null || s.monthLoaded,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateIfSession(seq) { it.copy(error = friendlyError(e)) }
            } finally {
                updateIfSession(seq) { it.copy(deletingIds = it.deletingIds - id) }
            }
        }
    }

    /** [id] == null creates. [onDone] runs after success. */
    fun saveAccount(id: String?, input: AccountInput, onDone: () -> Unit) =
        run(busy = true, sheet = true, mapError = ::accountError) { seq ->
            if (_state.value.isViewer) return@run
            refreshJob?.cancelAndJoin()
            storeMutex.withLock {
                val wsId = ensureWorkspace(seq)
                if (id == null) accountRepo.create(wsId, input) else accountRepo.update(wsId, id, input)
                coroutineScope {
                    val accDef = async { accountRepo.list(wsId, includeArchived = true) }
                    val balDef = async { safeResult { accountRepo.balances(wsId) } }
                    val accounts = accDef.await()
                    val balances = balDef.await().getOrNull()
                    updateIfSession(seq) { s ->
                        s.copy(
                            allAccounts = accounts,
                            accountsLoaded = true,
                            balances = balances ?: s.balances,
                            balancesLoaded = balances != null || s.balancesLoaded,
                        )
                    }
                }
            }
            if (sessionSeq == seq) onDone()
        }

    fun setAccountArchived(id: String, archived: Boolean, fromSheet: Boolean = false, onDone: () -> Unit = {}) =
        run(busy = true, sheet = fromSheet, mapError = ::accountError) { seq ->
            if (_state.value.isViewer) return@run
            refreshJob?.cancelAndJoin()
            storeMutex.withLock {
                val wsId = ensureWorkspace(seq)
                accountRepo.setArchived(wsId, id, archived)
                updateIfSession(seq) { s ->
                    s.copy(allAccounts = s.allAccounts.map { if (it.id == id) it.copy(archived = archived) else it })
                }
            }
            if (sessionSeq == seq) onDone()
        }

    fun createCategory(name: String, kind: String, icon: String, onDone: () -> Unit) =
        run(busy = true, sheet = true, mapError = ::categoryError) { seq ->
            if (_state.value.isViewer) return@run
            refreshJob?.cancelAndJoin()
            storeMutex.withLock {
                val wsId = ensureWorkspace(seq)
                categoryRepo.create(wsId, name.trim(), kind, icon)
                val categories = categoryRepo.listAll(wsId)
                updateIfSession(seq) { it.copy(allCategories = categories, categoriesLoaded = true, categoriesError = null) }
            }
            if (sessionSeq == seq) onDone()
        }

    fun updateCategory(id: String, name: String, icon: String, onDone: () -> Unit) =
        run(busy = true, sheet = true, mapError = ::categoryError) { seq ->
            if (_state.value.isViewer) return@run
            refreshJob?.cancelAndJoin()
            storeMutex.withLock {
                val wsId = ensureWorkspace(seq)
                categoryRepo.update(wsId, id, name.trim(), icon)
                val categories = categoryRepo.listAll(wsId)
                updateIfSession(seq) { it.copy(allCategories = categories, categoriesLoaded = true, categoriesError = null) }
            }
            if (sessionSeq == seq) onDone()
        }

    fun setCategoryArchived(id: String, archived: Boolean, onDone: () -> Unit = {}) =
        run(busy = true, sheet = true, mapError = ::categoryError) { seq ->
            if (_state.value.isViewer) return@run
            refreshJob?.cancelAndJoin()
            storeMutex.withLock {
                val wsId = ensureWorkspace(seq)
                categoryRepo.setArchived(wsId, id, archived)
                updateIfSession(seq) { s ->
                    s.copy(allCategories = s.allCategories.map { if (it.id == id) it.copy(archived = archived) else it })
                }
            }
            if (sessionSeq == seq) onDone()
        }

    fun clearError() = _state.update { it.copy(error = null, sheetError = null, notice = null) }

    private suspend fun refreshBalancesAndMonth(wsId: String): Pair<Map<String, AccountBalance>?, List<TxLite>?> =
        coroutineScope {
            val balDef = async { safeResult { accountRepo.balances(wsId) } }
            val monthDef = async { safeResult { dashboardRepo.currentMonth(wsId) } }
            balDef.await().getOrNull() to monthDef.await().getOrNull()
        }

    private suspend fun <T> safeResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun run(
        busy: Boolean = false,
        sheet: Boolean = false,
        sessionScoped: Boolean = true,
        mapError: (Throwable) -> String = ::friendlyError,
        block: suspend (Int) -> Unit,
    ) {
        if (busy && _state.value.busy) return
        val seq = sessionSeq
        val scope = if (sessionScoped) sessionScope else viewModelScope
        scope.launch {
            fun updateScoped(transform: (UiState) -> UiState) {
                if (sessionScoped) updateIfSession(seq, transform) else _state.update(transform)
            }
            if (busy) {
                updateScoped {
                    it.copy(
                        busy = true,
                        error = if (sheet) it.error else null,
                        sheetError = if (sheet) null else it.sheetError,
                        notice = null,
                    )
                }
            }
            try {
                block(seq)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = mapError(e)
                updateScoped { if (sheet) it.copy(sheetError = msg) else it.copy(error = msg) }
            } finally {
                if (busy) updateScoped { it.copy(busy = false) }
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
