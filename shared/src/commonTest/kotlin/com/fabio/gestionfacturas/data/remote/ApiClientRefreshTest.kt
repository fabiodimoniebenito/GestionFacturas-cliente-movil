package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.repository.AuthRepositoryImpl
import com.fabio.gestionfacturas.testutil.InMemoryTokenStorage
import com.fabio.gestionfacturas.testutil.LOGIN_OK_JSON
import com.fabio.gestionfacturas.testutil.cuerpoTexto
import com.fabio.gestionfacturas.testutil.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Bearer flow of the authenticated client: a 401 triggers POST /api/auth/refresh through the
 * PLAIN client, the stored refresh token is kept, and a failed refresh ends the session.
 */
class ApiClientRefreshTest {

    private val almacen = InMemoryTokenStorage()
    private val estado = EstadoSesion(inicial = true)
    private val peticiones = mutableListOf<HttpRequestData>()

    private fun cliente(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): ApiClient {
        val motor = MockEngine { peticion ->
            peticiones.add(peticion)
            manejador(peticion)
        }
        return ApiClient(baseUrl = "http://test", tokenStorage = almacen, estadoSesion = estado, engine = motor)
    }

    private fun HttpRequestData.autorizacion(): String? = headers["Authorization"]

    private fun rutas(): List<String> = peticiones.map { it.url.encodedPath }

    @Test
    fun un401RenuevaElAccessTokenYReintentaLaPeticion() = runTest {
        almacen.guardarSesion("viejo", "refresco", 7L, "ana@example.com")
        val api = cliente { peticion ->
            when {
                peticion.url.encodedPath == "/api/auth/refresh" -> respondJson("""{"accessToken":"nuevo"}""")
                peticion.autorizacion() == "Bearer nuevo" -> respondJson("[]")
                else -> respondJson("{}", HttpStatusCode.Unauthorized)
            }
        }

        val respuesta = api.authenticated.get("/api/clientes")

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(listOf("/api/clientes", "/api/auth/refresh", "/api/clientes"), rutas())
        assertEquals("Bearer viejo", peticiones[0].autorizacion())
        assertEquals("Bearer nuevo", peticiones[2].autorizacion())
    }

    @Test
    fun laRenovacionConservaElRefreshTokenGuardadoYUsaElClienteSinAuth() = runTest {
        almacen.guardarSesion("viejo", "refresco", 7L, "ana@example.com")
        val api = cliente { peticion ->
            when {
                peticion.url.encodedPath == "/api/auth/refresh" -> respondJson("""{"accessToken":"nuevo"}""")
                peticion.autorizacion() == "Bearer nuevo" -> respondJson("[]")
                else -> respondJson("{}", HttpStatusCode.Unauthorized)
            }
        }

        api.authenticated.get("/api/clientes")

        assertEquals("nuevo", almacen.accessToken())
        assertEquals("refresco", almacen.refreshToken())
        assertEquals(7L, almacen.usuarioId())
        assertEquals("ana@example.com", almacen.email())
        assertTrue(estado.activa.value)
        val refresco = peticiones.single { it.url.encodedPath == "/api/auth/refresh" }
        assertEquals("""{"refreshToken":"refresco"}""", refresco.cuerpoTexto())
        assertNull(refresco.autorizacion(), "el refresh va por el cliente plano, sin Authorization")
    }

    @Test
    fun siLaRenovacionFallaSeLimpiaLaSesionYSeMarcaInactiva() = runTest {
        almacen.guardarSesion("viejo", "refresco-caducado", 7L, "ana@example.com")
        val api = cliente { peticion ->
            if (peticion.url.encodedPath == "/api/auth/refresh") {
                respondJson("""{"error":"Refresh token inválido"}""", HttpStatusCode.Unauthorized)
            } else {
                respondJson("{}", HttpStatusCode.Unauthorized)
            }
        }

        val respuesta = api.authenticated.get("/api/clientes")

        assertEquals(HttpStatusCode.Unauthorized, respuesta.status)
        assertTrue(almacen.estaVacio())
        assertFalse(estado.activa.value)
    }

    @Test
    fun siLaRenovacionFallaPorRedTambienSeCierraLaSesion() = runTest {
        almacen.guardarSesion("viejo", "refresco", 7L, "ana@example.com")
        val api = cliente { peticion ->
            if (peticion.url.encodedPath == "/api/auth/refresh") {
                throw IOException("sin red")
            }
            respondJson("{}", HttpStatusCode.Unauthorized)
        }

        api.authenticated.get("/api/clientes")

        assertTrue(almacen.estaVacio())
        assertFalse(estado.activa.value)
    }

    @Test
    fun siLaRenovacionDevuelveUnaRespuestaSinTokenSeCierraLaSesion() = runTest {
        almacen.guardarSesion("viejo", "refresco", 7L, "ana@example.com")
        val api = cliente { peticion ->
            if (peticion.url.encodedPath == "/api/auth/refresh") {
                respondJson("{}")
            } else {
                respondJson("{}", HttpStatusCode.Unauthorized)
            }
        }

        api.authenticated.get("/api/clientes")

        assertTrue(almacen.estaVacio())
        assertFalse(estado.activa.value)
    }

    @Test
    fun sinTokensGuardadosNoSeEnviaAuthorizationYElFalloCierraLaSesion() = runTest {
        val api = cliente { respondJson("{}", HttpStatusCode.Unauthorized) }

        val respuesta = api.authenticated.get("/api/clientes")

        assertEquals(HttpStatusCode.Unauthorized, respuesta.status)
        assertNull(peticiones.first().autorizacion())
        assertFalse(estado.activa.value)
    }

    @Test
    fun unaPeticionCorrectaNoDisparaNingunaRenovacion() = runTest {
        almacen.guardarSesion("vigente", "refresco", 7L, "ana@example.com")
        val api = cliente { respondJson("[]") }

        val respuesta = api.authenticated.get("/api/clientes")

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(listOf("/api/clientes"), rutas())
        assertEquals("Bearer vigente", peticiones.single().autorizacion())
        assertTrue(estado.activa.value)
    }

    @Test
    fun trasElLogoutLaCacheDeKtorNoVuelveAEnviarElTokenAntiguo() = runTest {
        almacen.guardarSesion("viejo", "refresco", 7L, "ana@example.com")
        val api = cliente { respondJson("[]") }
        val repo = AuthRepositoryImpl(api, almacen, estado)

        api.authenticated.get("/api/clientes")
        repo.logout()
        api.authenticated.get("/api/clientes")

        val protegidas = peticiones.filter { it.url.encodedPath == "/api/clientes" }
        assertEquals("Bearer viejo", protegidas[0].autorizacion())
        assertNull(protegidas[1].autorizacion(), "tras el logout no debe salir ningún token")
    }

    @Test
    fun trasUnNuevoLoginSeUsaElTokenNuevoYNoElDeLaCache() = runTest {
        almacen.guardarSesion("viejo", "refresco-viejo", 1L, "otra@example.com")
        val api = cliente { peticion ->
            if (peticion.url.encodedPath == "/api/auth/login") respondJson(LOGIN_OK_JSON) else respondJson("[]")
        }
        val repo = AuthRepositoryImpl(api, almacen, estado)

        api.authenticated.get("/api/clientes")
        repo.login("ana@example.com", "secreta")
        api.authenticated.get("/api/clientes")

        val protegidas = peticiones.filter { it.url.encodedPath == "/api/clientes" }
        assertEquals("Bearer viejo", protegidas[0].autorizacion())
        assertEquals("Bearer acceso-1", protegidas[1].autorizacion())
    }
}
