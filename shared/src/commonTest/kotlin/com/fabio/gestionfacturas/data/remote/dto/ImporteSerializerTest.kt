package com.fabio.gestionfacturas.data.remote.dto

import com.fabio.gestionfacturas.domain.model.Importe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ImporteSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun leeUnNumeroJsonConservandoLosCerosFinales() {
        val respuesta = json.decodeFromString<GastoResponse>("""{"id":1,"baseImponible":121.00,"iva":21.5}""")

        assertEquals("121.00", respuesta.baseImponible?.valor)
        assertEquals("21.5", respuesta.iva?.valor)
    }

    @Test
    fun elRoundTripNoCambiaElLiteral() {
        val texto = """{"id":1,"baseImponible":121.00}"""

        val salida = json.encodeToString(GastoResponse.serializer(), json.decodeFromString(texto))

        assertEquals("""{"id":1,"baseImponible":121.00}""", salida)
    }

    @Test
    fun escribeUnNumeroJsonNoUnaCadena() {
        val request = CrearGastoRequest(total = Importe.de("121,00")!!)

        assertEquals("""{"total":121.00}""", json.encodeToString(CrearGastoRequest.serializer(), request))
    }

    @Test
    fun unEnteroSeConservaTalCual() {
        val respuesta = json.decodeFromString<GastoResponse>("""{"id":1,"iva":5}""")

        assertEquals("5", respuesta.iva?.valor)
    }

    @Test
    fun nuloYAusenteDanNull() {
        val respuesta = json.decodeFromString<GastoResponse>("""{"id":1,"iva":null}""")

        assertNull(respuesta.iva)
        assertNull(respuesta.baseImponible)
    }

    @Test
    fun unaCadenaJsonNoEsUnImporteValido() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<GastoResponse>("""{"id":1,"iva":"21.00"}""")
        }
    }
}
