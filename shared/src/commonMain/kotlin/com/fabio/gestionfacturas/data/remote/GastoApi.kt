package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.remote.dto.CrearGastoRequest
import com.fabio.gestionfacturas.data.remote.dto.EditarGastoRequest
import com.fabio.gestionfacturas.data.remote.dto.GastoResponse
import com.fabio.gestionfacturas.domain.model.ArchivoSeleccionado
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** The endpoints under `/api/gastos`. Must be given the AUTHENTICATED client (Bearer plugin). */
class GastoApi(private val client: HttpClient) {

    suspend fun listar(): List<GastoResponse> =
        ejecutarLlamada { client.get("/api/gastos") }

    suspend fun obtener(id: Long): GastoResponse =
        ejecutarLlamada { client.get("/api/gastos/$id") }

    suspend fun crear(request: CrearGastoRequest): GastoResponse =
        ejecutarLlamada {
            client.post("/api/gastos") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun editar(id: Long, request: EditarGastoRequest): GastoResponse =
        ejecutarLlamada {
            client.put("/api/gastos/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun revisar(id: Long): GastoResponse =
        ejecutarLlamada { client.post("/api/gastos/$id/revisar") }

    /**
     * Uploads one image as multipart (part `archivo`). The file travels as a ByteArray, never a stream,
     * so Ktor can replay the request after a token refresh. The body is never logged. The OCR is slow,
     * hence the longer timeout for this call only.
     */
    suspend fun digitalizar(archivo: ArchivoSeleccionado): GastoResponse {
        val respuesta = enviarLlamada {
            client.post("/api/gastos/digitalizar") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "archivo",
                                archivo.bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, archivo.mime)
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"${nombreSeguro(archivo.nombre)}\"",
                                    )
                                },
                            )
                        },
                    ),
                )
                timeout {
                    requestTimeoutMillis = TIMEOUT_DIGITALIZAR_MS
                    socketTimeoutMillis = TIMEOUT_DIGITALIZAR_MS
                }
            }
        }
        if (!respuesta.status.isSuccess()) {
            throw when (respuesta.status) {
                HttpStatusCode.PayloadTooLarge -> ErrorAuth.ArchivoDemasiadoGrande(camposDeError(respuesta)["error"])
                HttpStatusCode.UnprocessableEntity -> ErrorAuth.NoSePudoLeer(camposDeError(respuesta)["error"])
                else -> errorConDetalle(respuesta)
            }
        }
        try {
            return respuesta.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw ErrorAuth.Inesperado
        }
    }

    private companion object {
        const val TIMEOUT_DIGITALIZAR_MS = 90_000L
    }
}

/** Keeps the file name usable inside a quoted header value: no path, quotes or line breaks. */
internal fun nombreSeguro(nombre: String): String {
    val limpio = nombre.substringAfterLast('/').substringAfterLast(BARRA_INVERSA)
        .filter { it != '"' && it != SALTO_LINEA && it != RETORNO_CARRO }
        .trim()
    return limpio.ifEmpty { "archivo" }
}

private const val BARRA_INVERSA = '\\'
private const val SALTO_LINEA = '\n'
private const val RETORNO_CARRO = '\r'
