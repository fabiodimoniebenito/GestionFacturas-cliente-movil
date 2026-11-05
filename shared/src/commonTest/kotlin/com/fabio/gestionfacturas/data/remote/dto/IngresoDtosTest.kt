package com.fabio.gestionfacturas.data.remote.dto

import com.fabio.gestionfacturas.domain.model.Importe
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IngresoDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun laRespuestaConservaLosCerosDeTodosLosImportes() {
        val respuesta = json.decodeFromString<IngresoResponse>(
            """{"id":1,"baseImponible":100.00,"iva":21.00,"total":121.00}""",
        )

        assertEquals("100.00", respuesta.baseImponible?.valor)
        assertEquals("21.00", respuesta.iva?.valor)
        assertEquals("121.00", respuesta.total?.valor)
    }

    @Test
    fun elRoundTripDeLaRespuestaNoCambiaElLiteral() {
        val texto = """{"id":1,"total":121.00,"estadoCobro":"COBRADA","fechaCobro":"2026-11-01"}"""

        val salida = json.encodeToString(IngresoResponse.serializer(), json.decodeFromString(texto))

        assertEquals(texto, salida)
    }

    @Test
    fun laPeticionEscribeLosImportesComoNumerosJson() {
        val request = CrearIngresoRequest(
            total = Importe.de("121,00")!!,
            baseImponible = Importe.de("100")!!,
            iva = Importe.de("21.5")!!,
        )

        assertEquals(
            """{"total":121.00,"baseImponible":100,"iva":21.5}""",
            json.encodeToString(CrearIngresoRequest.serializer(), request),
        )
    }

    @Test
    fun fechaCobroNulaYAusenteDanNull() {
        assertNull(json.decodeFromString<IngresoResponse>("""{"id":1,"fechaCobro":null}""").fechaCobro)
        assertNull(json.decodeFromString<IngresoResponse>("""{"id":1}""").fechaCobro)
    }

    @Test
    fun registrarCobroSinFechaSeEscribeComoObjetoVacio() {
        assertEquals("{}", json.encodeToString(RegistrarCobroRequest.serializer(), RegistrarCobroRequest()))
    }
}
