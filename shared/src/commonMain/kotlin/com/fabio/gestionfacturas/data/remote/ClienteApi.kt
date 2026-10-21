package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.remote.dto.ClienteResponse
import com.fabio.gestionfacturas.data.remote.dto.CrearClienteRequest
import com.fabio.gestionfacturas.data.remote.dto.EditarClienteRequest
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * The endpoints under `/api/clientes`. Must be given the AUTHENTICATED client (Bearer plugin).
 * Every failure surfaces as an [ErrorAuth]; nothing here logs request or response bodies.
 */
class ClienteApi(private val client: HttpClient) {

    suspend fun listar(): List<ClienteResponse> =
        ejecutar { client.get("/api/clientes") }

    suspend fun obtener(id: Long): ClienteResponse =
        ejecutar { client.get("/api/clientes/$id") }

    suspend fun crear(request: CrearClienteRequest): ClienteResponse =
        ejecutar {
            client.post("/api/clientes") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun editar(id: Long, request: EditarClienteRequest): ClienteResponse =
        ejecutar {
            client.put("/api/clientes/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    /** DELETE deactivates the cliente; the response has no body. */
    suspend fun desactivar(id: Long) {
        val respuesta = enviar { client.delete("/api/clientes/$id") }
        if (!respuesta.status.isSuccess()) {
            throw errorDeRespuesta(respuesta)
        }
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
            HttpStatusCode.NotFound -> ErrorAuth.NoEncontrado
            HttpStatusCode.Conflict -> ErrorAuth.Conflicto
            HttpStatusCode.TooManyRequests ->
                ErrorAuth.DemasiadasPeticiones(respuesta.headers["Retry-After"]?.trim()?.toIntOrNull())
            HttpStatusCode.BadRequest -> ErrorAuth.Validacion(camposDeError(respuesta))
            // 401 here means the token refresh already failed and ended the session (see ApiClient).
            else -> ErrorAuth.Inesperado
        }
    }
}
