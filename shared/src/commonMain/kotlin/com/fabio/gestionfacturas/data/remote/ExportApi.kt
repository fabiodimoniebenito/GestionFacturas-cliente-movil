package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.domain.model.ArchivoExportado
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate

/** `/api/exportacion`. Must be given the AUTHENTICATED client. The body is binary and is never logged. */
class ExportApi(private val client: HttpClient) {

    suspend fun excel(desde: LocalDate, hasta: LocalDate): ArchivoExportado {
        val respuesta = enviarLlamada {
            client.get("/api/exportacion/excel") {
                parameter("desde", desde.toString())
                parameter("hasta", hasta.toString())
                // The endpoint only produces xlsx; the JSON plugin would otherwise send Accept: application/json (406).
                accept(ContentType.parse(MIME_XLSX))
                // Building the workbook can take longer than a JSON call: longer timeout for this call only.
                timeout {
                    requestTimeoutMillis = TIMEOUT_EXPORTACION_MS
                    socketTimeoutMillis = TIMEOUT_EXPORTACION_MS
                }
            }
        }
        if (!respuesta.status.isSuccess()) {
            throw errorConDetalle(respuesta)
        }
        val bytes = respuesta.bodyAsBytes()
        val nombre = nombreDeContentDisposition(respuesta.headers[HttpHeaders.ContentDisposition])
            ?: "gestionfacturas_${desde}_$hasta.xlsx"
        val mime = respuesta.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()
            ?.takeIf { it.isNotEmpty() } ?: MIME_XLSX
        return ArchivoExportado(nombre, bytes, mime)
    }

    private companion object {
        const val TIMEOUT_EXPORTACION_MS = 120_000L
    }
}

const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/** `filename="x.xlsx"` or `filename=x.xlsx`; path separators are dropped so the name is safe to propose. */
internal fun nombreDeContentDisposition(cabecera: String?): String? {
    if (cabecera == null) {
        return null
    }
    val resto = Regex("""filename\s*=\s*("([^"]*)"|[^;]+)""", RegexOption.IGNORE_CASE).find(cabecera) ?: return null
    val crudo = resto.groupValues[2].ifEmpty { resto.groupValues[1] }.trim().trim('"')
    val limpio = crudo.substringAfterLast('/').substringAfterLast('\\').trim()
    return limpio.ifEmpty { null }
}
