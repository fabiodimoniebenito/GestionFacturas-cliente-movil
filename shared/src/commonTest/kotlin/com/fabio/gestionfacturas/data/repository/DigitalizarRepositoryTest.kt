package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.data.remote.nombreSeguro
import com.fabio.gestionfacturas.domain.model.ArchivoSeleccionado
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.testutil.InMemoryTokenStorage
import com.fabio.gestionfacturas.testutil.respondJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** `GastoRepository.digitalizar` against a MockEngine over the AUTHENTICATED client. */
class DigitalizarRepositoryTest {

    private val peticiones = mutableListOf<HttpRequestData>()
    private val cuerpos = mutableListOf<ByteArray>()
    private val almacen = InMemoryTokenStorage(
        access = "acceso-1",
        refresh = "refresco-1",
        usuario = 7L,
        correo = "ana@example.com",
    )

    // Bytes that are not valid text (0x00, 0xFF, 0x0D 0x0A...) prove nothing re-encodes the file.
    private val bytesImagen = byteArrayOf(
        0x00, 0x7F, -1, -40, -1, -32, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x0D, 0x0A, -128,
    )
    private val archivo = ArchivoSeleccionado("factura.jpg", "image/jpeg", bytesImagen)

    private val gastoJson = """{"id":9,"usuarioId":7,"emisor":"Papeleria","fechaEmision":"2026-10-20",
        "baseImponible":100.00,"iva":21.00,"moneda":"EUR","estado":"BORRADOR","creadoEn":"2026-10-27T10:15:30"}"""

    private fun repo(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): GastoRepositoryImpl {
        val motor = MockEngine { peticion ->
            peticiones.add(peticion)
            if (peticion.url.encodedPath == "/api/gastos/digitalizar") {
                cuerpos.add(peticion.body.toByteArray())
            }
            manejador(peticion)
        }
        val cliente = ApiClient("http://test", almacen, EstadoSesion(inicial = true), engine = motor)
        return GastoRepositoryImpl(cliente.gastoApi)
    }

    private fun contiene(pajar: ByteArray, aguja: ByteArray): Boolean {
        for (i in 0..pajar.size - aguja.size) {
            var coincide = true
            for (j in aguja.indices) {
                if (pajar[i + j] != aguja[j]) {
                    coincide = false
                    break
                }
            }
            if (coincide) {
                return true
            }
        }
        return false
    }

    /** Latin-1 view of the bytes, to look for the multipart headers without losing binary bytes. */
    private fun ByteArray.comoTexto(): String = map { (it.toInt() and 0xFF).toChar() }.joinToString("")

    @Test
    fun enviaUnPostMultipartConLaParteArchivoYLosBytesIntactos() = runTest {
        val repositorio = repo { respondJson(gastoJson) }

        repositorio.digitalizar(archivo).getOrThrow()

        val peticion = peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertEquals("/api/gastos/digitalizar", peticion.url.encodedPath)
        assertIs<MultiPartFormDataContent>(peticion.body)
        assertTrue(peticion.body.contentType.toString().startsWith("multipart/form-data"))
        val cuerpo = cuerpos.single()
        assertTrue(cuerpo.comoTexto().contains("name=\"archivo\""), "la parte debe llamarse archivo")
        assertTrue(contiene(cuerpo, bytesImagen), "los bytes del fichero deben ir intactos en el cuerpo")
    }

    @Test
    fun enviaElNombreYElTipoDeContenidoDelArchivo() = runTest {
        val repositorio = repo { respondJson(gastoJson) }

        repositorio.digitalizar(ArchivoSeleccionado("recibo.png", "image/png", bytesImagen)).getOrThrow()

        val texto = cuerpos.single().comoTexto()
        assertTrue(texto.contains("filename=\"recibo.png\""), texto)
        assertTrue(texto.contains("Content-Type: image/png"), texto)
    }

    @Test
    fun laPeticionLlevaElBearer() = runTest {
        val repositorio = repo { respondJson(gastoJson) }

        repositorio.digitalizar(archivo).getOrThrow()

        assertEquals("Bearer acceso-1", peticiones.single().headers["Authorization"])
    }

    @Test
    fun laRespuestaSeMapeaAGastoBorrador() = runTest {
        val repositorio = repo { respondJson(gastoJson) }

        val gasto = repositorio.digitalizar(archivo).getOrThrow()

        assertEquals(9L, gasto.id)
        assertEquals("Papeleria", gasto.emisor)
        assertEquals("100.00", gasto.baseImponible?.valor)
        assertEquals("21.00", gasto.iva?.valor)
        assertEquals(EstadoGasto.BORRADOR, gasto.estado)
    }

    @Test
    fun un401SeguidoDeRefrescoExitosoReenviaLosMismosBytes() = runTest {
        val repositorio = repo { peticion ->
            when {
                peticion.url.encodedPath == "/api/auth/refresh" -> respondJson("""{"accessToken":"nuevo"}""")
                peticion.headers["Authorization"] == "Bearer nuevo" -> respondJson(gastoJson)
                else -> respondJson("{}", HttpStatusCode.Unauthorized)
            }
        }

        val gasto = repositorio.digitalizar(archivo).getOrThrow()

        assertEquals(9L, gasto.id)
        val subidas = peticiones.filter { it.url.encodedPath == "/api/gastos/digitalizar" }
        assertEquals(2, subidas.size)
        assertEquals("Bearer acceso-1", subidas[0].headers["Authorization"])
        assertEquals("Bearer nuevo", subidas[1].headers["Authorization"])
        assertEquals(2, cuerpos.size)
        assertTrue(contiene(cuerpos[0], bytesImagen), "primer envio")
        assertTrue(contiene(cuerpos[1], bytesImagen), "el reenvio debe llevar los mismos bytes")
        assertEquals(cuerpos[0].size, cuerpos[1].size)
    }

    @Test
    fun error400SeMapeaAValidacionYConservaElMensaje() = runTest {
        val repositorio = repo { respondJson("""{"error":"Archivo no válido"}""", HttpStatusCode.BadRequest) }

        val error = repositorio.digitalizar(archivo).exceptionOrNull()

        assertEquals(ErrorAuth.Validacion(mapOf("error" to "Archivo no válido")), error)
    }

    @Test
    fun error413SeMapeaAArchivoDemasiadoGrandeConElMensaje() = runTest {
        val repositorio = repo { respondJson("""{"error":"Máximo 10 MB"}""", HttpStatusCode.PayloadTooLarge) }

        val error = repositorio.digitalizar(archivo).exceptionOrNull()

        assertEquals(ErrorAuth.ArchivoDemasiadoGrande("Máximo 10 MB"), error)
    }

    @Test
    fun error422SeMapeaANoSePudoLeerConElMensaje() = runTest {
        val repositorio = repo {
            respondJson("""{"error":"No se pudo extraer texto"}""", HttpStatusCode.UnprocessableEntity)
        }

        val error = repositorio.digitalizar(archivo).exceptionOrNull()

        assertEquals(ErrorAuth.NoSePudoLeer("No se pudo extraer texto"), error)
    }

    @Test
    fun error413SinCuerpoDejaElDetalleVacio() = runTest {
        val repositorio = repo { respondJson("", HttpStatusCode.PayloadTooLarge) }

        assertEquals(ErrorAuth.ArchivoDemasiadoGrande(null), repositorio.digitalizar(archivo).exceptionOrNull())
    }

    @Test
    fun error429LlevaLaEspera() = runTest {
        val repositorio = repo { respondJson("{}", HttpStatusCode.TooManyRequests, retryAfter = "30") }

        assertEquals(ErrorAuth.DemasiadasPeticiones(30), repositorio.digitalizar(archivo).exceptionOrNull())
    }

    @Test
    fun unFalloDeRedEsServidorInaccesible() = runTest {
        val repositorio = repo { throw IOException("sin red") }

        assertEquals(ErrorAuth.ServidorInaccesible, repositorio.digitalizar(archivo).exceptionOrNull())
    }

    @Test
    fun otroEstadoEsInesperado() = runTest {
        val repositorio = repo { respondJson("{}", HttpStatusCode.InternalServerError) }

        assertEquals(ErrorAuth.Inesperado, repositorio.digitalizar(archivo).exceptionOrNull())
    }

    @Test
    fun nombreSeguroQuitaRutaComillasYSaltosDeLinea() {
        assertEquals("a.jpg", nombreSeguro("C:\\fotos\\a.jpg"))
        assertEquals("a.jpg", nombreSeguro("/tmp/x/a.jpg"))
        assertEquals("a.jpg", nombreSeguro("a\".j\r\npg"))
        assertEquals("archivo", nombreSeguro("  "))
    }
}
