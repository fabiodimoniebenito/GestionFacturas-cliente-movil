package com.fabio.gestionfacturas.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DashboardDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun elResumenConservaLosCerosYLosNegativos() {
        val texto = """{"facturadoTotal":121.00,"beneficioCajaTotal":-181.50}"""

        val respuesta = json.decodeFromString<ResumenPeriodoResponse>(texto)

        assertEquals("121.00", respuesta.facturadoTotal?.valor)
        assertEquals("-181.50", respuesta.beneficioCajaTotal?.valor)
        assertEquals(texto, json.encodeToString(ResumenPeriodoResponse.serializer(), respuesta))
    }

    @Test
    fun unaRentabilidadSinClienteIdDaNull() {
        val respuesta = json.decodeFromString<RentabilidadClienteResponse>("""{"resumen":{"gastosTotal":5.00}}""")

        assertNull(respuesta.clienteId)
        assertEquals("5.00", respuesta.resumen?.gastosTotal?.valor)
    }

    @Test
    fun pendientesSinIngresosEsUnaListaVacia() {
        val respuesta = json.decodeFromString<PendientesCobroResponse>("""{"ingresos":[],"total":0.00}""")

        assertEquals(emptyList(), respuesta.ingresos)
        assertEquals("0.00", respuesta.total?.valor)
    }

    @Test
    fun unImportePendienteEnComillasEsInvalido() {
        val resultado = runCatching {
            json.decodeFromString<PendientesCobroResponse>("""{"ingresos":[],"total":"12.00"}""")
        }

        assertEquals(true, resultado.isFailure)
    }
}
