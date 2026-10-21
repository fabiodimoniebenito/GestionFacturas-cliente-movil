package com.fabio.gestionfacturas.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Request JSON must match docs/api-contract.md exactly; responses are decoded from contract-shaped samples. */
class ClienteDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun crearRequestSeSerializaConLosNombresDelContrato() {
        val texto = json.encodeToString(
            CrearClienteRequest(nombre = "Acme", nif = "B12345678", email = "a@acme.com", telefono = "600111222")
        )

        assertEquals(
            """{"nombre":"Acme","nif":"B12345678","email":"a@acme.com","telefono":"600111222"}""",
            texto,
        )
    }

    @Test
    fun crearRequestOmiteEmailYTelefonoOpcionalesAusentes() {
        val texto = json.encodeToString(CrearClienteRequest(nombre = "Acme", nif = "B12345678"))

        assertEquals("""{"nombre":"Acme","nif":"B12345678"}""", texto)
    }

    @Test
    fun editarRequestNoIncluyeElNif() {
        val texto = json.encodeToString(EditarClienteRequest(nombre = "Acme SL", email = "a@acme.com"))

        assertEquals("""{"nombre":"Acme SL","email":"a@acme.com"}""", texto)
        assertFalse(texto.contains("nif"))
    }

    @Test
    fun clienteResponseCompletaSeDecodifica() {
        val cliente = json.decodeFromString<ClienteResponse>(
            """{"id":3,"usuarioId":7,"nombre":"Acme","nif":"B12345678","email":"a@acme.com","telefono":"600","activo":true}"""
        )

        assertEquals(3L, cliente.id)
        assertEquals(7L, cliente.usuarioId)
        assertEquals("Acme", cliente.nombre)
        assertEquals("B12345678", cliente.nif)
        assertEquals("a@acme.com", cliente.email)
        assertEquals("600", cliente.telefono)
        assertTrue(cliente.activo)
    }

    @Test
    fun clienteResponseSinEmailNiTelefonoOConNullSeDecodifica() {
        val sinCampos = json.decodeFromString<ClienteResponse>(
            """{"id":3,"nombre":"Acme","nif":"B1","activo":false}"""
        )
        val conNull = json.decodeFromString<ClienteResponse>(
            """{"id":3,"nombre":"Acme","nif":"B1","email":null,"telefono":null,"activo":true}"""
        )

        assertNull(sinCampos.email)
        assertNull(sinCampos.telefono)
        assertFalse(sinCampos.activo)
        assertNull(conNull.email)
        assertNull(conNull.telefono)
    }

    @Test
    fun clienteResponseIgnoraCamposDesconocidos() {
        val cliente = json.decodeFromString<ClienteResponse>(
            """{"id":3,"nombre":"Acme","nif":"B1","activo":true,"nuevo":"x"}"""
        )

        assertEquals(3L, cliente.id)
    }
}
