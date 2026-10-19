package com.fabio.gestionfacturas.data.remote

import co.touchlab.kermit.Logger
import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.local.TokenStorage
import com.fabio.gestionfacturas.data.remote.dto.RefreshRequest
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Builds the two Ktor clients the app uses:
 *  - [plain]: no auth plugin. For the public `/api/auth` endpoints and for the token refresh itself.
 *  - [authenticated]: Bearer plugin. For every protected endpoint (later slices).
 *
 * Deliberately NO logging plugin: request/response bodies and the Authorization header must never
 * be logged. [engine] is only injected by tests (MockEngine); in production Ktor picks OkHttp.
 */
class ApiClient(
    baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val estadoSesion: EstadoSesion,
    engine: HttpClientEngine? = null,
) {

    val plain: HttpClient = crearCliente(engine) { configurarComun(baseUrl) }

    val authApi: AuthApi = AuthApi(plain)

    val authenticated: HttpClient = crearCliente(engine) {
        configurarComun(baseUrl)
        install(Auth) {
            bearer {
                loadTokens {
                    val acceso = tokenStorage.accessToken()
                    val refresco = tokenStorage.refreshToken()
                    if (acceso != null && refresco != null) {
                        BearerTokens(acceso, refresco)
                    } else {
                        null
                    }
                }
                refreshTokens {
                    refrescarAccessToken()
                }
                // This client only ever talks to our own API, so always send the token. Ktor only
                // runs refreshTokens after a 401 for a request that already carried the token, and
                // the backend's 401 has no WWW-Authenticate challenge to trigger it otherwise.
                sendWithoutRequest { true }
            }
        }
    }

    /** Drops the access/refresh tokens Ktor's Bearer plugin has cached, so old credentials are not sent again. */
    fun limpiarTokensEnCache() {
        authenticated.clearAuthTokens()
    }

    /**
     * POST /api/auth/refresh through the PLAIN client with the stored refresh token. The endpoint
     * returns only an access token, so the stored refresh token is KEPT. Any failure ends the session.
     */
    private suspend fun refrescarAccessToken(): BearerTokens? {
        val refresco = tokenStorage.refreshToken()
        if (refresco == null) {
            cerrarSesionPorFalloDeRefresco()
            return null
        }
        try {
            val respuesta = authApi.refrescar(RefreshRequest(refresco))
            tokenStorage.guardarAccessToken(respuesta.accessToken)
            return BearerTokens(respuesta.accessToken, refresco)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            cerrarSesionPorFalloDeRefresco()
            return null
        }
    }

    private fun cerrarSesionPorFalloDeRefresco() {
        tokenStorage.limpiar()
        estadoSesion.marcarInactiva()
        // Never log tokens or messages here: only the fact that the session was lost.
        Logger.withTag("Auth").w { "No se pudo renovar el token: sesión cerrada" }
    }

    private fun crearCliente(engine: HttpClientEngine?, bloque: HttpClientConfig<*>.() -> Unit): HttpClient {
        if (engine == null) {
            return HttpClient(bloque)
        }
        return HttpClient(engine, bloque)
    }

    private fun HttpClientConfig<*>.configurarComun(baseUrl: String) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        defaultRequest {
            url(baseUrl)
        }
    }
}
