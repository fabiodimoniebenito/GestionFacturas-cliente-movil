package com.fabio.gestionfacturas.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Request JSON must match docs/api-contract.md exactly; responses are decoded from contract-shaped samples. */
class AuthDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun loginRequestSeSerializaConLosNombresDelContrato() {
        val texto = json.encodeToString(LoginRequest(email = "ana@example.com", password = "secreta"))

        assertEquals("""{"email":"ana@example.com","password":"secreta"}""", texto)
    }

    @Test
    fun registrarRequestSeSerializaConLosNombresDelContrato() {
        val texto = json.encodeToString(
            RegistrarRequest(email = "ana@example.com", nombre = "Ana", password = "secreta")
        )

        assertEquals("""{"email":"ana@example.com","nombre":"Ana","password":"secreta"}""", texto)
    }

    @Test
    fun refreshYLogoutRequestUsanRefreshToken() {
        assertEquals("""{"refreshToken":"r"}""", json.encodeToString(RefreshRequest("r")))
        assertEquals("""{"refreshToken":"r"}""", json.encodeToString(LogoutRequest("r")))
    }

    @Test
    fun loginResponseSeDecodificaDesdeLaMuestraDelContrato() {
        val respuesta = json.decodeFromString<LoginResponse>(
            """{"accessToken":"a","refreshToken":"r","usuarioId":42,"email":"ana@example.com"}"""
        )

        assertEquals(LoginResponse("a", "r", 42L, "ana@example.com"), respuesta)
    }

    @Test
    fun loginResponseIgnoraCamposDesconocidos() {
        val respuesta = json.decodeFromString<LoginResponse>(
            """{"accessToken":"a","refreshToken":"r","usuarioId":1,"email":"x@y.com","nuevoCampo":true}"""
        )

        assertEquals("a", respuesta.accessToken)
    }

    @Test
    fun refreshResponseSoloTraeElAccessToken() {
        val respuesta = json.decodeFromString<RefreshResponse>("""{"accessToken":"nuevo"}""")

        assertEquals("nuevo", respuesta.accessToken)
    }

    @Test
    fun usuarioResponseSeDecodificaYAdmiteCamposAusentes() {
        val completo = json.decodeFromString<UsuarioResponse>("""{"id":3,"email":"ana@example.com","nombre":"Ana"}""")
        val vacio = json.decodeFromString<UsuarioResponse>("{}")

        assertEquals(UsuarioResponse(3L, "ana@example.com", "Ana"), completo)
        assertNull(vacio.id)
        assertNull(vacio.email)
        assertNull(vacio.nombre)
    }
}
