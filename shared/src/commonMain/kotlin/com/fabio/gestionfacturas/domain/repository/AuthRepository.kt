package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Sesion
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication and session. Failures are reported as [ErrorAuth] inside the [Result].
 * Logout lives here (not in a use case) because this repository owns both the remote API and
 * the local token storage.
 */
interface AuthRepository {

    /** True while there is a usable session; turns false when the user logs out or a refresh fails. */
    val sesionActiva: StateFlow<Boolean>

    suspend fun login(email: String, password: String): Result<Sesion>

    /** Registration returns no tokens, so on success this logs in with the same credentials. */
    suspend fun registrar(email: String, nombre: String, password: String): Result<Sesion>

    /** Revokes the refresh token on the backend (best effort) and ALWAYS clears the local session. */
    suspend fun logout()

    /** The stored session, or null when there is none. */
    fun sesionActual(): Sesion?
}
