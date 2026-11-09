package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.data.remote.MIME_XLSX
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.testutil.InMemoryTokenStorage
import com.fabio.gestionfacturas.testutil.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Export repository against a MockEngine over the AUTHENTICATED client. */
class ExportRepositoryImplTest {

    private val peticiones = mutableListOf<HttpRequestData>()
    private val desde = LocalDate(2026, 1, 1)
    private val hasta = LocalDate(2026, 3, 31)

    // Bytes that are not valid UTF-8, to prove nothing decodes the body as text.
    private val binario = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0xFF.toByte(), 0xFE.toByte(), 0x80.toByte())

    private fun exportacion(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): ExportRepositoryImpl {
        val motor = MockEngine { peticion ->
            peticiones.add(peticion)
            manejador(peticion)
        }
        val cliente = ApiClient(
            baseUrl = "http://test",
            tokenStorage = InMemoryTokenStorage(access = "acceso-1", refresh = "refresco-1", usuario = 7L, correo = "ana@example.com"),
            estadoSesion = EstadoSesion(inicial = true),
            engine = motor,
        )
        return ExportRepositoryImpl(cliente.exportApi)
    }

    @Test
    fun devuelveLosBytesIntactosYElNombreDeLaCabecera() = runTest {
        val repo = exportacion {
            respond(
                content = binario,
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(MIME_XLSX),
                    HttpHeaders.ContentDisposition to listOf("attachment; filename=\"gestionfacturas_2026-01-01_2026-03-31.xlsx\""),
                ),
            )
        }

        val archivo = repo.exportarExcel(desde, hasta).getOrThrow()

        assertContentEquals(binario, archivo.bytes)
        assertEquals("gestionfacturas_2026-01-01_2026-03-31.xlsx", archivo.nombre)
        assertEquals(MIME_XLSX, archivo.mime)
    }

    @Test
    fun sinCabeceraUsaUnNombreDeRespaldoConElRango() = runTest {
        val repo = exportacion { respond(content = binario, status = HttpStatusCode.OK) }

        val archivo = repo.exportarExcel(desde, hasta).getOrThrow()

        assertEquals("gestionfacturas_2026-01-01_2026-03-31.xlsx", archivo.nombre)
        assertEquals(MIME_XLSX, archivo.mime)
    }

    @Test
    fun elNombreDeLaCabeceraNoPuedeTraerRutas() = runTest {
        val repo = exportacion {
            respond(
                content = binario,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentDisposition, "attachment; filename=\"../../etc/informe.xlsx\""),
            )
        }

        assertEquals("informe.xlsx", repo.exportarExcel(desde, hasta).getOrThrow().nombre)
    }

    @Test
    fun enviaGetConFechasYElTokenBearer() = runTest {
        val repo = exportacion { respond(content = binario, status = HttpStatusCode.OK) }

        repo.exportarExcel(desde, hasta)

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Get, peticion.method)
        assertEquals("/api/exportacion/excel", peticion.url.encodedPath)
        assertEquals("2026-01-01", peticion.url.parameters["desde"])
        assertEquals("2026-03-31", peticion.url.parameters["hasta"])
        assertEquals("Bearer acceso-1", peticion.headers[HttpHeaders.Authorization])
    }

    @Test
    fun un400SeMapeaAValidacionConElMensajeDelServidor() = runTest {
        val repo = exportacion { respondJson("""{"error":"La fecha 'desde' no puede ser posterior a 'hasta'"}""", HttpStatusCode.BadRequest) }

        val error = repo.exportarExcel(desde, hasta).exceptionOrNull()

        assertEquals(ErrorAuth.Validacion(mapOf("error" to "La fecha 'desde' no puede ser posterior a 'hasta'")), error)
    }

    @Test
    fun un429SeMapeaADemasiadasPeticionesConRetryAfter() = runTest {
        val repo = exportacion {
            respond(content = "", status = HttpStatusCode.TooManyRequests, headers = headersOf(HttpHeaders.RetryAfter, "12"))
        }

        assertEquals(ErrorAuth.DemasiadasPeticiones(12), repo.exportarExcel(desde, hasta).exceptionOrNull())
    }
}
