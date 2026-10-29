package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.domain.model.Categoria
import com.fabio.gestionfacturas.domain.model.DatosGasto
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.NuevoGasto
import com.fabio.gestionfacturas.testutil.InMemoryTokenStorage
import com.fabio.gestionfacturas.testutil.cuerpoTexto
import com.fabio.gestionfacturas.testutil.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Gasto and Categoria repositories against a MockEngine over the AUTHENTICATED client. */
class GastoRepositoryImplTest {

    private val peticiones = mutableListOf<HttpRequestData>()
    private lateinit var cliente: ApiClient

    private fun api(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
        val motor = MockEngine { peticion ->
            peticiones.add(peticion)
            manejador(peticion)
        }
        cliente = ApiClient(
            baseUrl = "http://test",
            tokenStorage = InMemoryTokenStorage(
                access = "acceso-1",
                refresh = "refresco-1",
                usuario = 7L,
                correo = "ana@example.com",
            ),
            estadoSesion = EstadoSesion(inicial = true),
            engine = motor,
        )
    }

    private fun gastos(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): GastoRepositoryImpl {
        api(manejador)
        return GastoRepositoryImpl(cliente.gastoApi)
    }

    private val gastoJson = """{"id":5,"usuarioId":7,"categoriaId":2,"clienteId":3,"emisor":"Papeleria",
        "fechaEmision":"2026-10-20","baseImponible":100.00,"iva":21.00,"moneda":"EUR","deducible":true,
        "estado":"BORRADOR","creadoEn":"2026-10-27T10:15:30.123456"}"""

    // ---- happy paths -------------------------------------------------------------------------

    @Test
    fun listarMapeaTodosLosCampos() = runTest {
        val repo = gastos { respondJson("[$gastoJson]") }

        val gasto = repo.listar().getOrThrow().single()

        assertEquals(HttpMethod.Get, peticiones.single().method)
        assertEquals("/api/gastos", peticiones.single().url.encodedPath)
        assertEquals(5L, gasto.id)
        assertEquals(7L, gasto.usuarioId)
        assertEquals(2L, gasto.categoriaId)
        assertEquals(3L, gasto.clienteId)
        assertEquals("Papeleria", gasto.emisor)
        assertEquals(LocalDate(2026, 10, 20), gasto.fechaEmision)
        assertEquals("100.00", gasto.baseImponible?.valor)
        assertEquals("21.00", gasto.iva?.valor)
        assertEquals("EUR", gasto.moneda)
        assertEquals(true, gasto.deducible)
        assertEquals(EstadoGasto.BORRADOR, gasto.estado)
        assertEquals(LocalDateTime(2026, 10, 27, 10, 15, 30, 123456000), gasto.creadoEn)
    }

    @Test
    fun unCreadoEnConZonaSeAceptaYSeIgnoraLaZona() = runTest {
        val repo = gastos { respondJson("""{"id":5,"creadoEn":"2026-10-27T10:15:30Z"}""") }

        assertEquals(LocalDateTime(2026, 10, 27, 10, 15, 30), repo.obtener(5L).getOrThrow().creadoEn)
    }

    @Test
    fun obtenerPideElGastoPorId() = runTest {
        val repo = gastos { respondJson(gastoJson) }

        val gasto = repo.obtener(5L).getOrThrow()

        assertEquals("/api/gastos/5", peticiones.single().url.encodedPath)
        assertEquals(5L, gasto.id)
    }

    @Test
    fun crearEnviaElTotalComoNumeroJsonYNoMandaLoNulo() = runTest {
        val repo = gastos { respondJson(gastoJson) }

        repo.crear(
            NuevoGasto(
                total = Importe.de("121,00")!!,
                emisor = "Papeleria",
                fechaEmision = LocalDate(2026, 10, 20),
                clienteId = 3L,
            )
        ).getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertEquals("/api/gastos", peticion.url.encodedPath)
        assertEquals(
            """{"total":121.00,"emisor":"Papeleria","fechaEmision":"2026-10-20","clienteId":3}""",
            peticion.cuerpoTexto(),
        )
    }

    @Test
    fun editarEnviaCategoriaYDeducible() = runTest {
        val repo = gastos { respondJson(gastoJson) }

        repo.editar(5L, DatosGasto(total = Importe.de("121.00")!!, categoriaId = 2L, deducible = true)).getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Put, peticion.method)
        assertEquals("/api/gastos/5", peticion.url.encodedPath)
        assertEquals("""{"total":121.00,"categoriaId":2,"deducible":true}""", peticion.cuerpoTexto())
    }

    @Test
    fun revisarHaceUnPostSinCuerpo() = runTest {
        val repo = gastos { respondJson(gastoJson.replace("BORRADOR", "REVISADA")) }

        val gasto = repo.revisar(5L).getOrThrow()

        assertEquals(HttpMethod.Post, peticiones.single().method)
        assertEquals("/api/gastos/5/revisar", peticiones.single().url.encodedPath)
        assertEquals(EstadoGasto.REVISADA, gasto.estado)
    }

    @Test
    fun listarCategorias() = runTest {
        api { respondJson("""[{"id":1,"nombre":"Material","deduciblePorDefecto":true},{"id":2,"nombre":"Ocio","deduciblePorDefecto":false}]""") }

        val categorias = CategoriaRepositoryImpl(cliente.categoriaApi).listar().getOrThrow()

        assertEquals("/api/categorias", peticiones.single().url.encodedPath)
        assertEquals(listOf(Categoria(1L, "Material", true), Categoria(2L, "Ocio", false)), categorias)
    }

    @Test
    fun lasPeticionesLlevanElBearer() = runTest {
        val repo = gastos { respondJson("[]") }

        repo.listar().getOrThrow()

        assertEquals("Bearer acceso-1", peticiones.single().headers["Authorization"])
    }

    // ---- errors ------------------------------------------------------------------------------

    @Test
    fun error400SeMapeaAMapaDeCampos() = runTest {
        val repo = gastos { respondJson("""{"total":"debe ser mayor o igual que 0"}""", HttpStatusCode.BadRequest) }

        val resultado = repo.crear(NuevoGasto(total = Importe.de("1")!!))

        assertEquals(
            ErrorAuth.Validacion(mapOf("total" to "debe ser mayor o igual que 0")),
            resultado.exceptionOrNull(),
        )
    }

    @Test
    fun error404ConservaElMensajeDelBackend() = runTest {
        val repo = gastos { respondJson("""{"error":"Categoría no encontrada"}""", HttpStatusCode.NotFound) }

        val resultado = repo.editar(5L, DatosGasto(total = Importe.de("1")!!, categoriaId = 99L))

        assertEquals(ErrorAuth.NoEncontradoConDetalle("Categoría no encontrada"), resultado.exceptionOrNull())
    }

    @Test
    fun error404SinCuerpoDejaElDetalleVacio() = runTest {
        val repo = gastos { respondJson("", HttpStatusCode.NotFound) }

        assertEquals(ErrorAuth.NoEncontradoConDetalle(null), repo.obtener(1L).exceptionOrNull())
    }

    @Test
    fun error409EnEditarYRevisarConservaElMensaje() = runTest {
        val repo = gastos { respondJson("""{"error":"El gasto ya está revisado"}""", HttpStatusCode.Conflict) }

        val enEditar = repo.editar(5L, DatosGasto(total = Importe.de("1")!!))
        val enRevisar = repo.revisar(5L)

        assertEquals(ErrorAuth.ConflictoConDetalle("El gasto ya está revisado"), enEditar.exceptionOrNull())
        assertEquals(ErrorAuth.ConflictoConDetalle("El gasto ya está revisado"), enRevisar.exceptionOrNull())
    }

    @Test
    fun unEstadoDesconocidoEsInesperado() = runTest {
        val repo = gastos { respondJson("""{"id":5,"estado":"OTRO"}""") }

        assertEquals(ErrorAuth.Inesperado, repo.obtener(5L).exceptionOrNull())
    }

    @Test
    fun error429LlevaLaEspera() = runTest {
        val repo = gastos { respondJson("{}", HttpStatusCode.TooManyRequests, retryAfter = "12") }

        assertEquals(ErrorAuth.DemasiadasPeticiones(12), repo.listar().exceptionOrNull())
    }

    @Test
    fun categoriasTambienMapeanErrores() = runTest {
        api { respondJson("{}", HttpStatusCode.InternalServerError) }

        assertTrue(CategoriaRepositoryImpl(cliente.categoriaApi).listar().exceptionOrNull() == ErrorAuth.Inesperado)
    }
}
