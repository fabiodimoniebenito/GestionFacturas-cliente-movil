package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.testutil.InMemoryTokenStorage
import com.fabio.gestionfacturas.testutil.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Dashboard repository against a MockEngine over the AUTHENTICATED client. */
class DashboardRepositoryImplTest {

    private val peticiones = mutableListOf<HttpRequestData>()
    private val desde = LocalDate(2026, 1, 1)
    private val hasta = LocalDate(2026, 1, 31)

    private fun dashboard(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): DashboardRepositoryImpl {
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
        return DashboardRepositoryImpl(cliente.dashboardApi)
    }

    // Ten DISTINCT values, in the order the contract lists them, to catch cross-assigned fields.
    private val resumenJson = """{"facturadoTotal":1.00,"facturadoBase":2.00,"cobradoTotal":3.00,
        "cobradoBase":4.00,"gastosTotal":5.00,"gastosBase":6.00,"beneficioCajaTotal":7.00,
        "beneficioCajaBase":8.00,"beneficioFacturadoTotal":9.00,"beneficioFacturadoBase":10.00}"""

    // ---- resumen -----------------------------------------------------------------------------

    @Test
    fun resumenMapeaCadaCampoPorSeparado() = runTest {
        val repo = dashboard { respondJson(resumenJson) }

        val resumen = repo.resumen(desde, hasta).getOrThrow()

        assertEquals("1.00", resumen.facturadoTotal.valor)
        assertEquals("2.00", resumen.facturadoBase.valor)
        assertEquals("3.00", resumen.cobradoTotal.valor)
        assertEquals("4.00", resumen.cobradoBase.valor)
        assertEquals("5.00", resumen.gastosTotal.valor)
        assertEquals("6.00", resumen.gastosBase.valor)
        assertEquals("7.00", resumen.beneficioCajaTotal.valor)
        assertEquals("8.00", resumen.beneficioCajaBase.valor)
        assertEquals("9.00", resumen.beneficioFacturadoTotal.valor)
        assertEquals("10.00", resumen.beneficioFacturadoBase.valor)
    }

    @Test
    fun resumenEnviaLasFechasEnIsoYUsaGet() = runTest {
        val repo = dashboard { respondJson(resumenJson) }

        repo.resumen(desde, hasta).getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Get, peticion.method)
        assertEquals("/api/dashboard/resumen", peticion.url.encodedPath)
        assertEquals("2026-01-01", peticion.url.parameters["desde"])
        assertEquals("2026-01-31", peticion.url.parameters["hasta"])
    }

    @Test
    fun resumenConBeneficioNegativoLoLeeSinFallarYConservaElLiteral() = runTest {
        val repo = dashboard {
            respondJson(resumenJson.replace(""""beneficioCajaTotal":7.00""", """"beneficioCajaTotal":-181.50"""))
        }

        val resumen = repo.resumen(desde, hasta).getOrThrow()

        assertEquals("-181.50", resumen.beneficioCajaTotal.valor)
        assertEquals(true, resumen.beneficioCajaTotal.esNegativo)
        assertEquals("-181,50", resumen.beneficioCajaTotal.mostrar())
        assertEquals(false, resumen.beneficioCajaBase.esNegativo)
    }

    @Test
    fun resumenSinUnImporteEsUnaRespuestaInesperada() = runTest {
        val repo = dashboard { respondJson("""{"facturadoTotal":1.00}""") }

        assertEquals(ErrorAuth.Inesperado, repo.resumen(desde, hasta).exceptionOrNull())
    }

    // ---- rentabilidad ------------------------------------------------------------------------

    @Test
    fun rentabilidadMapeaClienteYResumenYEnviaLasFechas() = runTest {
        val repo = dashboard { respondJson("""[{"clienteId":3,"resumen":$resumenJson}]""") }

        val entrada = repo.rentabilidad(desde, hasta).getOrThrow().single()

        val peticion = peticiones.single()
        assertEquals("/api/dashboard/rentabilidad-clientes", peticion.url.encodedPath)
        assertEquals("2026-01-01", peticion.url.parameters["desde"])
        assertEquals("2026-01-31", peticion.url.parameters["hasta"])
        assertEquals(3L, entrada.clienteId)
        assertNull(entrada.clienteNombre)
        assertEquals("1.00", entrada.resumen.facturadoTotal.valor)
        assertEquals("10.00", entrada.resumen.beneficioFacturadoBase.valor)
    }

    @Test
    fun laEntradaSinClienteTieneIdYNombreNulos() = runTest {
        val repo = dashboard { respondJson("""[{"resumen":$resumenJson},{"clienteId":3,"resumen":$resumenJson}]""") }

        val entradas = repo.rentabilidad(desde, hasta).getOrThrow()

        assertEquals(2, entradas.size)
        assertNull(entradas[0].clienteId)
        assertNull(entradas[0].clienteNombre)
        assertEquals(3L, entradas[1].clienteId)
    }

    @Test
    fun rentabilidadVaciaEsUnaListaVacia() = runTest {
        val repo = dashboard { respondJson("[]") }

        assertEquals(emptyList(), repo.rentabilidad(desde, hasta).getOrThrow())
    }

    // ---- pendientes --------------------------------------------------------------------------

    @Test
    fun pendientesMapeaLosCamposYNoLlevaParametros() = runTest {
        val repo = dashboard {
            respondJson(
                """{"ingresos":[{"id":9,"clienteId":3,"concepto":"Diseno web","fechaEmision":"2026-10-20",
                "baseImponible":100.00,"iva":21.00,"total":121.00}],"total":121.00}""",
            )
        }

        val pendientes = repo.pendientesCobro().getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Get, peticion.method)
        assertEquals("/api/dashboard/pendientes-cobro", peticion.url.encodedPath)
        assertEquals(true, peticion.url.parameters.isEmpty())
        assertEquals("121.00", pendientes.total.valor)
        val item = pendientes.ingresos.single()
        assertEquals(9L, item.id)
        assertEquals(3L, item.clienteId)
        assertEquals("Diseno web", item.concepto)
        assertEquals(LocalDate(2026, 10, 20), item.fechaEmision)
        assertEquals("100.00", item.baseImponible?.valor)
        assertEquals("21.00", item.iva?.valor)
        assertEquals("121.00", item.total?.valor)
    }

    @Test
    fun pendientesVaciosDanListaVaciaYTotalCero() = runTest {
        val repo = dashboard { respondJson("""{"ingresos":[],"total":0.00}""") }

        val pendientes = repo.pendientesCobro().getOrThrow()

        assertEquals(emptyList(), pendientes.ingresos)
        assertEquals("0.00", pendientes.total.valor)
    }

    @Test
    fun pendientesSinTotalEsUnaRespuestaInesperada() = runTest {
        val repo = dashboard { respondJson("""{"ingresos":[]}""") }

        assertEquals(ErrorAuth.Inesperado, repo.pendientesCobro().exceptionOrNull())
    }

    // ---- errors ------------------------------------------------------------------------------

    @Test
    fun un400SeMapeaAValidacionConElMapaDeCampos() = runTest {
        val repo = dashboard { respondJson("""{"hasta":"debe ser posterior a desde"}""", HttpStatusCode.BadRequest) }

        assertEquals(
            ErrorAuth.Validacion(mapOf("hasta" to "debe ser posterior a desde")),
            repo.resumen(desde, hasta).exceptionOrNull(),
        )
    }

    @Test
    fun un400ConMensajeSimpleSeGuardaBajoError() = runTest {
        val repo = dashboard { respondJson("""{"error":"Rango de fechas inválido"}""", HttpStatusCode.BadRequest) }

        assertEquals(
            ErrorAuth.Validacion(mapOf("error" to "Rango de fechas inválido")),
            repo.rentabilidad(desde, hasta).exceptionOrNull(),
        )
    }

    @Test
    fun un429SeMapeaADemasiadasPeticiones()= runTest {
        val repo = dashboard { respondJson("", HttpStatusCode.TooManyRequests) }

        assertEquals(ErrorAuth.DemasiadasPeticiones(null), repo.pendientesCobro().exceptionOrNull())
    }
}
