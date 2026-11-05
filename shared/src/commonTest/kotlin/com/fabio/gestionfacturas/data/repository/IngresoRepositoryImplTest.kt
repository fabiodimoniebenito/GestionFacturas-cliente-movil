package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.NuevoIngreso
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
import kotlin.test.assertNull

/** Ingreso repository against a MockEngine over the AUTHENTICATED client. */
class IngresoRepositoryImplTest {

    private val peticiones = mutableListOf<HttpRequestData>()

    private fun ingresos(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): IngresoRepositoryImpl {
        val motor = MockEngine { peticion ->
            peticiones.add(peticion)
            manejador(peticion)
        }
        val cliente = ApiClient(
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
        return IngresoRepositoryImpl(cliente.ingresoApi)
    }

    private val ingresoJson = """{"id":9,"usuarioId":7,"clienteId":3,"concepto":"Diseno web",
        "fechaEmision":"2026-10-20","baseImponible":100.00,"iva":21.00,"total":121.00,"moneda":"EUR",
        "estadoCobro":"PENDIENTE","fechaCobro":null,"creadoEn":"2026-10-27T10:15:30.123456"}"""

    private val ingresoCobradoJson = """{"id":9,"estadoCobro":"COBRADA","fechaCobro":"2026-11-01"}"""

    // ---- happy paths -------------------------------------------------------------------------

    @Test
    fun listarMapeaTodosLosCampos() = runTest {
        val repo = ingresos { respondJson("[$ingresoJson]") }

        val ingreso = repo.listar().getOrThrow().single()

        assertEquals(HttpMethod.Get, peticiones.single().method)
        assertEquals("/api/ingresos", peticiones.single().url.encodedPath)
        assertEquals(9L, ingreso.id)
        assertEquals(7L, ingreso.usuarioId)
        assertEquals(3L, ingreso.clienteId)
        assertEquals("Diseno web", ingreso.concepto)
        assertEquals(LocalDate(2026, 10, 20), ingreso.fechaEmision)
        assertEquals("100.00", ingreso.baseImponible?.valor)
        assertEquals("21.00", ingreso.iva?.valor)
        assertEquals("121.00", ingreso.total?.valor)
        assertEquals("EUR", ingreso.moneda)
        assertEquals(EstadoCobro.PENDIENTE, ingreso.estadoCobro)
        assertNull(ingreso.fechaCobro)
        assertEquals(LocalDateTime(2026, 10, 27, 10, 15, 30, 123456000), ingreso.creadoEn)
    }

    @Test
    fun obtenerPideElIngresoPorId() = runTest {
        val repo = ingresos { respondJson(ingresoJson) }

        assertEquals(9L, repo.obtener(9L).getOrThrow().id)
        assertEquals("/api/ingresos/9", peticiones.single().url.encodedPath)
    }

    @Test
    fun crearEnviaElTotalComoNumeroJsonYOmiteLoOpcional() = runTest {
        val repo = ingresos { respondJson(ingresoJson) }

        repo.crear(NuevoIngreso(total = Importe.de("121,00")!!, concepto = "Diseno web")).getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertEquals("/api/ingresos", peticion.url.encodedPath)
        assertEquals("""{"total":121.00,"concepto":"Diseno web"}""", peticion.cuerpoTexto())
    }

    @Test
    fun registrarCobroConFechaLaEnviaEnIso() = runTest {
        val repo = ingresos { respondJson(ingresoCobradoJson) }

        val ingreso = repo.registrarCobro(9L, LocalDate(2026, 11, 1)).getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertEquals("/api/ingresos/9/cobro", peticion.url.encodedPath)
        assertEquals("""{"fechaCobro":"2026-11-01"}""", peticion.cuerpoTexto())
        assertEquals(EstadoCobro.COBRADA, ingreso.estadoCobro)
        assertEquals(LocalDate(2026, 11, 1), ingreso.fechaCobro)
    }

    @Test
    fun registrarCobroSinFechaEnviaUnObjetoVacio() = runTest {
        val repo = ingresos { respondJson(ingresoCobradoJson) }

        repo.registrarCobro(9L).getOrThrow()

        assertEquals("{}", peticiones.single().cuerpoTexto())
    }

    @Test
    fun revertirCobroHaceUnDeleteYMapeaLaRespuesta() = runTest {
        val repo = ingresos { respondJson(ingresoJson) }

        val ingreso = repo.revertirCobro(9L).getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Delete, peticion.method)
        assertEquals("/api/ingresos/9/cobro", peticion.url.encodedPath)
        assertEquals(EstadoCobro.PENDIENTE, ingreso.estadoCobro)
        assertNull(ingreso.fechaCobro)
    }

    // ---- unknown values ----------------------------------------------------------------------

    @Test
    fun unEstadoCobroDesconocidoNoRompe() = runTest {
        val repo = ingresos { respondJson("""{"id":9,"estadoCobro":"ANULADA"}""") }

        assertEquals(EstadoCobro.DESCONOCIDO, repo.obtener(9L).getOrThrow().estadoCobro)
    }

    @Test
    fun sinEstadoCobroQuedaNulo() = runTest {
        val repo = ingresos { respondJson("""{"id":9}""") }

        assertNull(repo.obtener(9L).getOrThrow().estadoCobro)
    }

    // ---- errors ------------------------------------------------------------------------------

    @Test
    fun un400SeMapeaAValidacionConElMapaDeCampos() = runTest {
        val repo = ingresos { respondJson("""{"total":"debe ser mayor o igual que 0"}""", HttpStatusCode.BadRequest) }

        val resultado = repo.crear(NuevoIngreso(total = Importe.de("1")!!))

        assertEquals(
            ErrorAuth.Validacion(mapOf("total" to "debe ser mayor o igual que 0")),
            resultado.exceptionOrNull(),
        )
    }

    @Test
    fun un404ConservaElMensajeDelBackend() = runTest {
        val repo = ingresos { respondJson("""{"error":"Cliente no encontrado"}""", HttpStatusCode.NotFound) }

        val resultado = repo.crear(NuevoIngreso(total = Importe.de("1")!!, clienteId = 99L))

        assertEquals(ErrorAuth.NoEncontradoConDetalle("Cliente no encontrado"), resultado.exceptionOrNull())
    }

    @Test
    fun un404SinCuerpoDaDetalleNulo() = runTest {
        val repo = ingresos { respondJson("", HttpStatusCode.NotFound) }

        assertEquals(ErrorAuth.NoEncontradoConDetalle(null), repo.obtener(1L).exceptionOrNull())
    }

    @Test
    fun un409ConservaElMensajeEnRegistrarYRevertirCobro() = runTest {
        val repo = ingresos { respondJson("""{"error":"El ingreso ya está cobrado"}""", HttpStatusCode.Conflict) }

        val enRegistrar = repo.registrarCobro(9L)
        val enRevertir = repo.revertirCobro(9L)

        assertEquals(ErrorAuth.ConflictoConDetalle("El ingreso ya está cobrado"), enRegistrar.exceptionOrNull())
        assertEquals(ErrorAuth.ConflictoConDetalle("El ingreso ya está cobrado"), enRevertir.exceptionOrNull())
    }

    @Test
    fun unaFechaIlegibleEsUnaRespuestaInesperada() = runTest {
        val repo = ingresos { respondJson("""{"id":9,"fechaCobro":"ayer"}""") }

        assertEquals(ErrorAuth.Inesperado, repo.obtener(9L).exceptionOrNull())
    }
}
