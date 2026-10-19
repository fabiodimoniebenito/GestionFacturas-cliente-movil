package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.model.Sesion
import com.fabio.gestionfacturas.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AuthRepository for ViewModel tests: no network, programmable results. Set [puerta] to keep a
 * call suspended so the test can observe the loading state, then complete it.
 */
class FakeAuthRepository : AuthRepository {

    private val _sesionActiva = MutableStateFlow(false)
    override val sesionActiva: StateFlow<Boolean> = _sesionActiva

    var resultadoLogin: Result<Sesion> = Result.success(Sesion(1L, "ana@example.com"))
    var resultadoRegistro: Result<Sesion> = Result.success(Sesion(1L, "ana@example.com"))
    var puerta: CompletableDeferred<Unit>? = null
    var sesion: Sesion? = null

    val llamadasLogin = mutableListOf<Pair<String, String>>()
    val llamadasRegistro = mutableListOf<Triple<String, String, String>>()
    var logouts = 0

    override suspend fun login(email: String, password: String): Result<Sesion> {
        llamadasLogin.add(email to password)
        puerta?.await()
        return resultadoLogin
    }

    override suspend fun registrar(email: String, nombre: String, password: String): Result<Sesion> {
        llamadasRegistro.add(Triple(email, nombre, password))
        puerta?.await()
        return resultadoRegistro
    }

    override suspend fun logout() {
        logouts++
        puerta?.await()
        sesion = null
        _sesionActiva.value = false
    }

    override fun sesionActual(): Sesion? = sesion
}
