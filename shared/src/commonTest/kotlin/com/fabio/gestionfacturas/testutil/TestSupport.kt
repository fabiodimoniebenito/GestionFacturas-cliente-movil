package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.data.local.TokenStorage
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent

/** TokenStorage kept in memory, so tests never touch real Settings. */
class InMemoryTokenStorage(
    var access: String? = null,
    var refresh: String? = null,
    var usuario: Long? = null,
    var correo: String? = null,
) : TokenStorage {

    override fun guardarSesion(accessToken: String, refreshToken: String, usuarioId: Long, email: String) {
        access = accessToken
        refresh = refreshToken
        usuario = usuarioId
        correo = email
    }

    override fun guardarAccessToken(accessToken: String) {
        access = accessToken
    }

    override fun accessToken(): String? = access
    override fun refreshToken(): String? = refresh
    override fun usuarioId(): Long? = usuario
    override fun email(): String? = correo

    override fun limpiar() {
        access = null
        refresh = null
        usuario = null
        correo = null
    }

    fun estaVacio(): Boolean = access == null && refresh == null && usuario == null && correo == null
}

/** JSON response for MockEngine handlers. */
fun MockRequestHandleScope.respondJson(
    cuerpo: String,
    estado: HttpStatusCode = HttpStatusCode.OK,
    retryAfter: String? = null,
): HttpResponseData {
    val cabeceras = mutableMapOf(HttpHeaders.ContentType to listOf("application/json"))
    if (retryAfter != null) {
        cabeceras["Retry-After"] = listOf(retryAfter)
    }
    return respond(content = cuerpo, status = estado, headers = headersOf(*cabeceras.map { it.key to it.value }.toTypedArray()))
}

/** The request body as the exact JSON text Ktor serialized. */
fun HttpRequestData.cuerpoTexto(): String = (body as TextContent).text

const val LOGIN_OK_JSON =
    """{"accessToken":"acceso-1","refreshToken":"refresco-1","usuarioId":7,"email":"ana@example.com"}"""

const val REGISTRO_OK_JSON = """{"id":7,"email":"ana@example.com","nombre":"Ana"}"""
