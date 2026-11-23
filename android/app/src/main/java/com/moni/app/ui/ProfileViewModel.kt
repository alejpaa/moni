package com.moni.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moni.app.data.Category
import com.moni.app.data.CategoryRepository
import com.moni.app.data.Profile
import com.moni.app.data.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val profile: Profile? = null,
    /** Active and archived. */
    val categories: List<Category> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class ProfileViewModel : ViewModel() {
    private val profiles = ProfileRepository()
    private val categoriesRepo = CategoryRepository()
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    private var loaded = false

    fun loadOnce() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            runCatching { profiles.load() }.onSuccess { p -> _state.update { it.copy(profile = p) } }
            runCatching { categoriesRepo.listAll() }.onSuccess { c -> _state.update { it.copy(categories = c) } }
                .onFailure { e -> if (e is CancellationException) throw e; _state.update { it.copy(error = friendlyError(e)) } }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, notice = null) }

    fun saveName(name: String, onDone: () -> Unit) = run("Nombre actualizado", onDone) {
        profiles.updateName(name.trim())
        val p = profiles.load()
        _state.update { it.copy(profile = p) }
    }

    fun changePassword(password: String, onDone: () -> Unit) = run("Contraseña actualizada", onDone) {
        profiles.updatePassword(password)
    }

    fun createCategory(name: String, kind: String, icon: String, onDone: () -> Unit, changed: () -> Unit) =
        run(null, onDone, changed) { categoriesRepo.create(name.trim(), kind, icon); reloadCategories() }

    fun updateCategory(id: String, name: String, icon: String, onDone: () -> Unit, changed: () -> Unit) =
        run(null, onDone, changed) { categoriesRepo.update(id, name.trim(), icon); reloadCategories() }

    fun setArchived(id: String, archived: Boolean, onDone: () -> Unit, changed: () -> Unit) =
        run(null, onDone, changed) { categoriesRepo.setArchived(id, archived); reloadCategories() }

    private suspend fun reloadCategories() {
        val c = categoriesRepo.listAll()
        _state.update { it.copy(categories = c) }
    }

    private fun run(
        notice: String?,
        onDone: () -> Unit,
        changed: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(busy = false, notice = notice) }
                changed()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message.orEmpty().lowercase()
                val text = if ("23505" in msg || "duplicate key" in msg) "Ya existe una categoría con ese nombre" else friendlyError(e)
                _state.update { it.copy(busy = false, error = text) }
            }
        }
    }
}
