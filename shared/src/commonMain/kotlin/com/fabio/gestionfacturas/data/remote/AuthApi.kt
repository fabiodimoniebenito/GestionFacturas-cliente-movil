package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.remote.dto.LoginRequest
import com.fabio.gestionfacturas.data.remote.dto.LoginResponse
import com.fabio.gestionfacturas.data.remote.dto.LogoutRequest
import com.fabio.gestionfacturas.data.remote.dto.RefreshRequest
import com.fabio.gestionfacturas.data.remote.dto.RefreshResponse
import com.fabio.gestionfacturas.data.remote.dto.RegistrarRequest
import com.fabio.gestionfacturas.data.remote.dto.UsuarioResponse
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The auth endpoints under `/api/auth`. Always goes through the PLAIN client (no Bearer plugin): these
 * routes are public and the refresh call must not recurse into the auth machinery.
 * Every failure surfaces as an [ErrorAuth]; nothing here logs request or response bodies.
 */
class AuthApi(private val client: HttpClient) {

    suspend fun registrar(request: RegistrarRequest): UsuarioResponse =
        ejecutar { client.post("/api/auth/register") { json(request) } }

    suspend fun login(request: LoginRequest): LoginResponse =
        ejecutar { client.post("/api/auth/login") { json(request) } }

    suspend fun refrescar(request: RefreshRequest): RefreshResponse =
        ejecutar { client.post("/api/auth/refresh") { json(request) } }

    suspend fun logout(request: LogoutRequest) {
        val respuesta = enviar { client.post("/api/auth/logout") { json(request) } }
        if (!respuesta.status.isSuccess()) {
            throw errorDeRespuesta(respuesta)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.json(cuerpo: Any) {
        contentType(ContentType.Application.Json)
        setBody(cuerpo)
    }

    private suspend inline fun <reified T> ejecutar(crossinline llamada: suspend () -> HttpResponse): T {
        val respuesta = enviar { llamada() }
        if (!respuesta.status.isSuccess()) {
            throw errorDeRespuesta(respuesta)
        }
        try {
            return respuesta.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw ErrorAuth.Inesperado
        }
    }

    private suspend fun enviar(llamada: suspend () -> HttpResponse): HttpResponse {
        try {
            return llamada()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Connection refused, DNS, timeouts (Ktor's timeout exceptions are IOExceptions)
            throw ErrorAuth.ServidorInaccesible
        } catch (e: Throwable) {
            throw ErrorAuth.Inesperado
        }
    }

    private suspend fun errorDeRespuesta(respuesta: HttpResponse): ErrorAuth {
        return when (respuesta.status) {
            HttpStatusCode.Unauthorized -> ErrorAuth.Credenciales
            HttpStatusCode.Conflict -> ErrorAuth.EmailYaRegistrado
            HttpStatusCode.TooManyRequests ->
                ErrorAuth.DemasiadasPeticiones(respuesta.headers["Retry-After"]?.trim()?.toIntOrNull())
            HttpStatusCode.BadRequest -> ErrorAuth.Validacion(camposDeError(respuesta))
            else -> ErrorAuth.Inesperado
        }
    }

    /** 400 bodies are a field -> message map (validation) or `{"error": "..."}` (domain rule). */
    private suspend fun camposDeError(respuesta: HttpResponse): Map<String, String> {
        val cuerpo = try {
            respuesta.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return emptyMap()
        }
        val raiz = try {
            Json.parseToJsonElement(cuerpo)
        } catch (e: Exception) {
            return emptyMap()
        }
        if (raiz !is JsonObject) {
            return emptyMap()
        }
        val campos = mutableMapOf<String, String>()
        for ((clave, valor) in raiz) {
            if (valor is JsonPrimitive) {
                campos[clave] = valor.content
            }
        }
        return campos
    }
}
