package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * Shared plumbing for the APIs on the AUTHENTICATED client: sends the call, maps transport
 * failures and non-2xx statuses to [ErrorAuth], decodes the body. 404 and 409 keep the backend's
 * `error` message. Nothing here logs request or response bodies.
 */
internal suspend inline fun <reified T> ejecutarLlamada(crossinline llamada: suspend () -> HttpResponse): T {
    val respuesta = enviarLlamada { llamada() }
    if (!respuesta.status.isSuccess()) {
        throw errorConDetalle(respuesta)
    }
    try {
        return respuesta.body()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ErrorAuth.Inesperado
    }
}

internal suspend fun enviarLlamada(llamada: suspend () -> HttpResponse): HttpResponse {
    try {
        return llamada()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        throw ErrorAuth.ServidorInaccesible
    } catch (e: Throwable) {
        throw ErrorAuth.Inesperado
    }
}

internal suspend fun errorConDetalle(respuesta: HttpResponse): ErrorAuth {
    return when (respuesta.status) {
        HttpStatusCode.NotFound -> ErrorAuth.NoEncontradoConDetalle(camposDeError(respuesta)["error"])
        HttpStatusCode.Conflict -> ErrorAuth.ConflictoConDetalle(camposDeError(respuesta)["error"])
        HttpStatusCode.TooManyRequests ->
            ErrorAuth.DemasiadasPeticiones(respuesta.headers["Retry-After"]?.trim()?.toIntOrNull())
        HttpStatusCode.BadRequest -> ErrorAuth.Validacion(camposDeError(respuesta))
        // 401 here means the token refresh already failed and ended the session (see ApiClient).
        else -> ErrorAuth.Inesperado
    }
}
