package com.moni.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val loading: Boolean = false,
    val loadError: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class ProfileViewModel : ViewModel() {
    private val profiles = ProfileRepository()
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    private var loadedSessionKey: Int = -1

    fun loadOnce(sessionKey: Int) {
        if (loadedSessionKey == sessionKey && _state.value.loadError == null) return
        _state.value = ProfileState(loading = true)
        viewModelScope.launch {
            try {
                val p = profiles.load()
                loadedSessionKey = sessionKey
                _state.update { it.copy(profile = p, loading = false, loadError = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = friendlyError(e)) }
            }
        }
    }

    fun reset() {
        loadedSessionKey = -1
        _state.value = ProfileState()
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

    private fun run(
        notice: String?,
        onDone: () -> Unit,
        block: suspend () -> Unit,
    ) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(busy = false, notice = notice) }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = friendlyError(e)) }
            }
        }
    }
}
