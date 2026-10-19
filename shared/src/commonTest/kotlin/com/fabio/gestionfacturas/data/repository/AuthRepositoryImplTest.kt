package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Sesion
import com.fabio.gestionfacturas.testutil.InMemoryTokenStorage
import com.fabio.gestionfacturas.testutil.LOGIN_OK_JSON
import com.fabio.gestionfacturas.testutil.REGISTRO_OK_JSON
import com.fabio.gestionfacturas.testutil.cuerpoTexto
import com.fabio.gestionfacturas.testutil.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** AuthRepositoryImpl against a Ktor MockEngine: no network, no real Settings. */
class AuthRepositoryImplTest {

    private val almacen = InMemoryTokenStorage()
    private val estado = EstadoSesion(inicial = false)
    private val peticiones = mutableListOf<HttpRequestData>()

    private fun repositorio(manejador: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData): AuthRepositoryImpl {
        val motor = MockEngine { peticion ->
            peticiones.add(peticion)
            manejador(peticion)
        }
        val cliente = ApiClient(baseUrl = "http://test", tokenStorage = almacen, estadoSesion = estado, engine = motor)
        return AuthRepositoryImpl(cliente, almacen, estado)
    }

    @Test
    fun loginCorrectoGuardaLosTokensYActivaLaSesion() = runTest {
        val repo = repositorio { respondJson(LOGIN_OK_JSON) }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(Sesion(7L, "ana@example.com"), resultado.getOrNull())
        assertEquals("acceso-1", almacen.accessToken())
        assertEquals("refresco-1", almacen.refreshToken())
        assertEquals(7L, almacen.usuarioId())
        assertEquals("ana@example.com", almacen.email())
        assertTrue(repo.sesionActiva.value)
    }

    @Test
    fun loginEnviaPostAlEndpointDelContratoConElJsonExacto() = runTest {
        val repo = repositorio { respondJson(LOGIN_OK_JSON) }

        repo.login("ana@example.com", "secreta")

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertEquals("/api/auth/login", peticion.url.encodedPath)
        assertEquals("""{"email":"ana@example.com","password":"secreta"}""", peticion.cuerpoTexto())
        assertNull(peticion.headers["Authorization"], "el login no debe enviar Authorization")
    }

    @Test
    fun login401SeMapeaACredencialesYNoGuardaNada() = runTest {
        val repo = repositorio { respondJson("""{"error":"Email incorrecto"}""", HttpStatusCode.Unauthorized) }

        val resultado = repo.login("ana@example.com", "mala")

        assertEquals(ErrorAuth.Credenciales, resultado.exceptionOrNull())
        assertTrue(almacen.estaVacio())
        assertFalse(repo.sesionActiva.value)
    }

    @Test
    fun login429ConRetryAfterIncluyeLaEspera() = runTest {
        val repo = repositorio { respondJson("{}", HttpStatusCode.TooManyRequests, retryAfter = "60") }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.DemasiadasPeticiones(60), resultado.exceptionOrNull())
    }

    @Test
    fun login429SinRetryAfterNoInventaLaEspera() = runTest {
        val repo = repositorio { respondJson("{}", HttpStatusCode.TooManyRequests) }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.DemasiadasPeticiones(null), resultado.exceptionOrNull())
    }

    @Test
    fun login429ConRetryAfterNoNumericoNoInventaLaEspera() = runTest {
        val repo = repositorio {
            respondJson("{}", HttpStatusCode.TooManyRequests, retryAfter = "Wed, 21 Oct 2026 07:28:00 GMT")
        }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.DemasiadasPeticiones(null), resultado.exceptionOrNull())
    }

    @Test
    fun error400ConMapaDeCamposSeMapeaAValidacion() = runTest {
        val repo = repositorio {
            respondJson("""{"email":"debe ser un correo válido","password":"obligatoria"}""", HttpStatusCode.BadRequest)
        }

        val resultado = repo.login("x", "")

        assertEquals(
            ErrorAuth.Validacion(mapOf("email" to "debe ser un correo válido", "password" to "obligatoria")),
            resultado.exceptionOrNull(),
        )
    }

    @Test
    fun error400ConMensajeDeDominioSeGuardaBajoLaClaveError() = runTest {
        val repo = repositorio { respondJson("""{"error":"Dato no válido"}""", HttpStatusCode.BadRequest) }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.Validacion(mapOf("error" to "Dato no válido")), resultado.exceptionOrNull())
    }

    @Test
    fun error400ConCuerpoNoJsonDaValidacionVacia() = runTest {
        val repo = repositorio { respond(content = "<html>", status = HttpStatusCode.BadRequest) }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.Validacion(emptyMap()), resultado.exceptionOrNull())
    }

    @Test
    fun registro409SeMapeaAEmailYaRegistradoYNoIntentaElLogin() = runTest {
        val repo = repositorio { respondJson("""{"error":"Email ya registrado"}""", HttpStatusCode.Conflict) }

        val resultado = repo.registrar("ana@example.com", "Ana", "secreta")

        assertEquals(ErrorAuth.EmailYaRegistrado, resultado.exceptionOrNull())
        assertEquals(listOf("/api/auth/register"), peticiones.map { it.url.encodedPath })
        assertTrue(almacen.estaVacio())
    }

    @Test
    fun registroCorrectoHaceLoginConLasMismasCredenciales() = runTest {
        val repo = repositorio { peticion ->
            if (peticion.url.encodedPath == "/api/auth/register") {
                respondJson(REGISTRO_OK_JSON)
            } else {
                respondJson(LOGIN_OK_JSON)
            }
        }

        val resultado = repo.registrar("ana@example.com", "Ana", "secreta")

        assertEquals(Sesion(7L, "ana@example.com"), resultado.getOrNull())
        assertEquals(listOf("/api/auth/register", "/api/auth/login"), peticiones.map { it.url.encodedPath })
        assertEquals(
            """{"email":"ana@example.com","nombre":"Ana","password":"secreta"}""",
            peticiones[0].cuerpoTexto(),
        )
        assertEquals("""{"email":"ana@example.com","password":"secreta"}""", peticiones[1].cuerpoTexto())
        assertEquals("acceso-1", almacen.accessToken())
        assertTrue(repo.sesionActiva.value)
    }

    @Test
    fun fallosDeRedSeMapeanAServidorInaccesible() = runTest {
        val repo = repositorio { throw IOException("sin red") }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.ServidorInaccesible, resultado.exceptionOrNull())
        assertTrue(almacen.estaVacio())
    }

    @Test
    fun unEstadoInesperadoSeMapeaAInesperado() = runTest {
        val repo = repositorio { respondJson("{}", HttpStatusCode.InternalServerError) }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.Inesperado, resultado.exceptionOrNull())
    }

    @Test
    fun unLoginSinTokensEnLaRespuestaEsInesperadoYNoGuardaNada() = runTest {
        val repo = repositorio { respondJson("""{"usuarioId":7,"email":"ana@example.com"}""") }

        val resultado = repo.login("ana@example.com", "secreta")

        assertEquals(ErrorAuth.Inesperado, resultado.exceptionOrNull())
        assertTrue(almacen.estaVacio())
        assertFalse(repo.sesionActiva.value)
    }

    @Test
    fun logoutRevocaElRefreshTokenYLimpiaLaSesion() = runTest {
        almacen.guardarSesion("acceso-1", "refresco-1", 7L, "ana@example.com")
        estado.marcarActiva()
        val repo = repositorio { respond(content = "", status = HttpStatusCode.OK) }

        repo.logout()

        val peticion = peticiones.single()
        assertEquals("/api/auth/logout", peticion.url.encodedPath)
        assertEquals("""{"refreshToken":"refresco-1"}""", peticion.cuerpoTexto())
        assertTrue(almacen.estaVacio())
        assertFalse(repo.sesionActiva.value)
    }

    @Test
    fun logoutLimpiaLosTokensLocalesAunqueFalleLaRed() = runTest {
        almacen.guardarSesion("acceso-1", "refresco-1", 7L, "ana@example.com")
        estado.marcarActiva()
        val repo = repositorio { throw IOException("sin red") }

        repo.logout()

        assertTrue(almacen.estaVacio())
        assertFalse(repo.sesionActiva.value)
    }

    @Test
    fun logoutLimpiaLosTokensLocalesAunqueElBackendRespondaError() = runTest {
        almacen.guardarSesion("acceso-1", "refresco-1", 7L, "ana@example.com")
        estado.marcarActiva()
        val repo = repositorio { respondJson("{}", HttpStatusCode.InternalServerError) }

        repo.logout()

        assertTrue(almacen.estaVacio())
        assertFalse(repo.sesionActiva.value)
    }

    @Test
    fun logoutSinSesionNoLlamaAlBackendPeroDejaTodoLimpio() = runTest {
        val repo = repositorio { respond(content = "", status = HttpStatusCode.OK) }

        repo.logout()

        assertTrue(peticiones.isEmpty())
        assertFalse(repo.sesionActiva.value)
    }

    @Test
    fun sesionActualDevuelveLaSesionGuardadaONulo() = runTest {
        val repo = repositorio { respond(content = "", status = HttpStatusCode.OK) }
        assertNull(repo.sesionActual())

        almacen.guardarSesion("a", "r", 9L, "bea@example.com")

        assertEquals(Sesion(9L, "bea@example.com"), repo.sesionActual())
    }
}
