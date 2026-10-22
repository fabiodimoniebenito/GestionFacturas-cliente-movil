package com.fabio.gestionfacturas.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShellViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShellUiState(email = authRepository.sesionActual()?.email.orEmpty()))
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()

    /** Clears the session; the navigation reacts to `sesionActiva` turning false. */
    fun onCerrarSesion() {
        if (_uiState.value.cerrandoSesion) {
            return
        }
        _uiState.update { it.copy(cerrandoSesion = true) }
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(cerrandoSesion = false) }
        }
    }
}
