package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.local.TokenStorage
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.data.remote.dto.LoginRequest
import com.fabio.gestionfacturas.data.remote.dto.LogoutRequest
import com.fabio.gestionfacturas.data.remote.dto.RegistrarRequest
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Sesion
import com.fabio.gestionfacturas.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage,
    private val estadoSesion: EstadoSesion,
) : AuthRepository {

    override val sesionActiva: StateFlow<Boolean> = estadoSesion.activa

    override suspend fun login(email: String, password: String): Result<Sesion> {
        try {
            val respuesta = apiClient.authApi.login(LoginRequest(email, password))
            tokenStorage.guardarSesion(
                accessToken = respuesta.accessToken,
                refreshToken = respuesta.refreshToken,
                usuarioId = respuesta.usuarioId,
                email = respuesta.email,
            )
            // A previous session may have left tokens in Ktor's cache: make it reload the new ones.
            apiClient.limpiarTokensEnCache()
            estadoSesion.marcarActiva()
            return Result.success(Sesion(respuesta.usuarioId, respuesta.email))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorAuth) {
            return Result.failure(e)
        } catch (e: Throwable) {
            return Result.failure(ErrorAuth.Inesperado)
        }
    }

    override suspend fun registrar(email: String, nombre: String, password: String): Result<Sesion> {
        try {
            apiClient.authApi.registrar(RegistrarRequest(email = email, nombre = nombre, password = password))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorAuth) {
            return Result.failure(e)
        } catch (e: Throwable) {
            return Result.failure(ErrorAuth.Inesperado)
        }
        // Registration returns no tokens: log in with the same credentials.
        return login(email, password)
    }

    override suspend fun logout() {
        // Must complete even if the caller is cancelled (e.g. the screen goes away).
        withContext(NonCancellable) {
            val refresco = tokenStorage.refreshToken()
            if (refresco != null) {
                try {
                    apiClient.authApi.logout(LogoutRequest(refresco))
                } catch (e: Throwable) {
                    // Best effort: the local session is cleared below whatever the backend says.
                }
            }
            tokenStorage.limpiar()
            apiClient.limpiarTokensEnCache()
            estadoSesion.marcarInactiva()
        }
    }

    override fun sesionActual(): Sesion? {
        val usuarioId = tokenStorage.usuarioId()
        val email = tokenStorage.email()
        if (usuarioId == null || email == null) {
            return null
        }
        return Sesion(usuarioId, email)
    }
}
