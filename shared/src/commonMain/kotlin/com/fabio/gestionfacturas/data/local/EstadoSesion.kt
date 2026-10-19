package com.fabio.gestionfacturas.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for "is there a usable session". Shared by the repository (login/logout)
 * and the API client (a failed token refresh ends the session), and exposed through
 * `AuthRepository.sesionActiva`.
 */
class EstadoSesion(inicial: Boolean) {

    private val _activa = MutableStateFlow(inicial)
    val activa: StateFlow<Boolean> = _activa.asStateFlow()

    fun marcarActiva() {
        _activa.value = true
    }

    fun marcarInactiva() {
        _activa.value = false
    }
}
