package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.DatosCliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.NuevoCliente
import com.fabio.gestionfacturas.testutil.InMemoryTokenStorage
import com.fabio.gestionfacturas.testutil.cuerpoTexto
import com.fabio.gestionfacturas.testutil.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ClienteRepositoryImpl against a Ktor MockEngine over the AUTHENTICATED client: no network, no real Settings. */
class ClienteRepositoryImplTest {

    private val almacen = InMemoryTokenStorage(
        access = "acceso-1",
        refresh = "refresco-1",
        usuario = 7L,
        correo = "ana@example.com",
    )
    private val peticiones = mutableListOf<HttpRequestData>()

    private fun repositorio(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): ClienteRepositoryImpl {
        val motor = MockEngine { peticion ->
            peticiones.add(peticion)
            manejador(peticion)
        }
        val cliente = ApiClient(
            baseUrl = "http://test",
            tokenStorage = almacen,
            estadoSesion = EstadoSesion(inicial = true),
            engine = motor,
        )
        return ClienteRepositoryImpl(cliente.clienteApi)
    }

    private val acme = Cliente(
        id = 3L,
        nombre = "Acme",
        nif = "B12345678",
        email = "a@acme.com",
        telefono = "600111222",
        activo = true,
    )

    // ---- happy paths -------------------------------------------------------------------------

    @Test
    fun listarDevuelveLosClientesTalComoLosDevuelveElBackendIncluidosLosInactivos() = runTest {
        val repo = repositorio {
            respondJson(
                """[
                    {"id":3,"usuarioId":7,"nombre":"Acme","nif":"B12345678","email":"a@acme.com","telefono":"600111222","activo":true},
                    {"id":4,"usuarioId":7,"nombre":"Vieja SL","nif":"A1","activo":false}
                ]"""
            )
        }

        val resultado = repo.listar()

        assertEquals(
            listOf(acme, Cliente(4L, "Vieja SL", "A1", null, null, false)),
            resultado.getOrNull(),
        )
        val peticion = peticiones.single()
        assertEquals(HttpMethod.Get, peticion.method)
        assertEquals("/api/clientes", peticion.url.encodedPath)
    }

    @Test
    fun obtenerPideElClientePorId() = runTest {
        val repo = repositorio {
            respondJson("""{"id":3,"usuarioId":7,"nombre":"Acme","nif":"B12345678","email":"a@acme.com","telefono":"600111222","activo":true}""")
        }

        val resultado = repo.obtener(3L)

        assertEquals(acme, resultado.getOrNull())
        val peticion = peticiones.single()
        assertEquals(HttpMethod.Get, peticion.method)
        assertEquals("/api/clientes/3", peticion.url.encodedPath)
    }

    @Test
    fun crearEnviaPostConElJsonDelContratoYDevuelveElClienteCreado() = runTest {
        val repo = repositorio {
            respondJson("""{"id":3,"usuarioId":7,"nombre":"Acme","nif":"B12345678","email":"a@acme.com","telefono":"600111222","activo":true}""")
        }

        val resultado = repo.crear(NuevoCliente("Acme", "B12345678", "a@acme.com", "600111222"))

        assertEquals(acme, resultado.getOrNull())
        val peticion = peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertEquals("/api/clientes", peticion.url.encodedPath)
        assertEquals(
            """{"nombre":"Acme","nif":"B12345678","email":"a@acme.com","telefono":"600111222"}""",
            peticion.cuerpoTexto(),
        )
    }

    @Test
    fun editarEnviaPutSinNifYDevuelveElClienteActualizado() = runTest {
        val repo = repositorio {
            respondJson("""{"id":3,"usuarioId":7,"nombre":"Acme SL","nif":"B12345678","activo":true}""")
        }

        val resultado = repo.editar(3L, DatosCliente(nombre = "Acme SL", email = null, telefono = null))

        assertEquals(Cliente(3L, "Acme SL", "B12345678", null, null, true), resultado.getOrNull())
        val peticion = peticiones.single()
        assertEquals(HttpMethod.Put, peticion.method)
        assertEquals("/api/clientes/3", peticion.url.encodedPath)
        assertEquals("""{"nombre":"Acme SL"}""", peticion.cuerpoTexto())
        assertTrue(!peticion.cuerpoTexto().contains("nif"))
    }

    @Test
    fun desactivarEnviaDeleteYAceptaUnaRespuestaSinCuerpo() = runTest {
        val repo = repositorio { respond(content = "", status = HttpStatusCode.OK) }

        val resultado = repo.desactivar(3L)

        assertTrue(resultado.isSuccess)
        val peticion = peticiones.single()
        assertEquals(HttpMethod.Delete, peticion.method)
        assertEquals("/api/clientes/3", peticion.url.encodedPath)
    }

    // ---- Authorization -----------------------------------------------------------------------

    @Test
    fun todasLasOperacionesEnvianElBearerToken() = runTest {
        val repo = repositorio { peticion ->
            when (peticion.method) {
                HttpMethod.Delete -> respond(content = "", status = HttpStatusCode.OK)
                else -> respondJson(
                    if (peticion.url.encodedPath == "/api/clientes" && peticion.method == HttpMethod.Get) {
                        "[]"
                    } else {
                        """{"id":3,"nombre":"Acme","nif":"B1","activo":true}"""
                    }
                )
            }
        }

        repo.listar()
        repo.obtener(3L)
        repo.crear(NuevoCliente("Acme", "B1"))
        repo.editar(3L, DatosCliente("Acme"))
        repo.desactivar(3L)

        assertEquals(5, peticiones.size)
        for (peticion in peticiones) {
            assertEquals("Bearer acceso-1", peticion.headers["Authorization"], "${peticion.method} ${peticion.url.encodedPath}")
        }
    }

    // ---- errors ------------------------------------------------------------------------------

    @Test
    fun error400ConMapaDeCamposSeMapeaAValidacion() = runTest {
        val repo = repositorio {
            respondJson("""{"nombre":"no debe estar vacío","email":"no es válido"}""", HttpStatusCode.BadRequest)
        }

        val resultado = repo.crear(NuevoCliente("", "B1", "mal"))

        assertEquals(
            ErrorAuth.Validacion(mapOf("nombre" to "no debe estar vacío", "email" to "no es válido")),
            resultado.exceptionOrNull(),
        )
    }

    @Test
    fun error400ConCampoErrorSeGuardaBajoLaClaveError() = runTest {
        val repo = repositorio { respondJson("""{"error":"NIF no válido"}""", HttpStatusCode.BadRequest) }

        val resultado = repo.crear(NuevoCliente("Acme", "xx"))

        assertEquals(ErrorAuth.Validacion(mapOf("error" to "NIF no válido")), resultado.exceptionOrNull())
    }

    @Test
    fun error404SeMapeaANoEncontradoEnObtenerEditarYDesactivar() = runTest {
        val repo = repositorio { respondJson("""{"error":"Cliente no encontrado"}""", HttpStatusCode.NotFound) }

        assertEquals(ErrorAuth.NoEncontrado, repo.obtener(99L).exceptionOrNull())
        assertEquals(ErrorAuth.NoEncontrado, repo.editar(99L, DatosCliente("Acme")).exceptionOrNull())
        assertEquals(ErrorAuth.NoEncontrado, repo.desactivar(99L).exceptionOrNull())
    }

    @Test
    fun error409SeMapeaAConflicto() = runTest {
        val repo = repositorio { respondJson("""{"error":"NIF duplicado"}""", HttpStatusCode.Conflict) }

        val resultado = repo.crear(NuevoCliente("Acme", "B12345678"))

        assertEquals(ErrorAuth.Conflicto, resultado.exceptionOrNull())
    }

    @Test
    fun error429LlevaLaEsperaDeRetryAfter() = runTest {
        val repo = repositorio { respondJson("{}", HttpStatusCode.TooManyRequests, retryAfter = "30") }

        val resultado = repo.listar()

        assertEquals(ErrorAuth.DemasiadasPeticiones(30), resultado.exceptionOrNull())
    }

    @Test
    fun fallosDeRedSeMapeanAServidorInaccesible() = runTest {
        val repo = repositorio { throw IOException("sin red") }

        val resultado = repo.listar()

        assertEquals(ErrorAuth.ServidorInaccesible, resultado.exceptionOrNull())
    }

    @Test
    fun error500SeMapeaAInesperado() = runTest {
        val repo = repositorio { respondJson("{}", HttpStatusCode.InternalServerError) }

        assertEquals(ErrorAuth.Inesperado, repo.listar().exceptionOrNull())
    }

    @Test
    fun respuestaSinCamposObligatoriosSeMapeaAInesperado() = runTest {
        val repo = repositorio { respondJson("""{"id":3}""") }

        val resultado = repo.obtener(3L)

        assertEquals(ErrorAuth.Inesperado, resultado.exceptionOrNull())
        assertNull(resultado.getOrNull())
    }
}
