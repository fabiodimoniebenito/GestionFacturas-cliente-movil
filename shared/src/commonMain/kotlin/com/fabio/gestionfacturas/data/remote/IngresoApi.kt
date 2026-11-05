package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.remote.dto.CrearIngresoRequest
import com.fabio.gestionfacturas.data.remote.dto.IngresoResponse
import com.fabio.gestionfacturas.data.remote.dto.RegistrarCobroRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** The endpoints under `/api/ingresos`. Must be given the AUTHENTICATED client (Bearer plugin). */
class IngresoApi(private val client: HttpClient) {

    suspend fun listar(): List<IngresoResponse> =
        ejecutarLlamada { client.get("/api/ingresos") }

    suspend fun obtener(id: Long): IngresoResponse =
        ejecutarLlamada { client.get("/api/ingresos/$id") }

    suspend fun crear(request: CrearIngresoRequest): IngresoResponse =
        ejecutarLlamada {
            client.post("/api/ingresos") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun registrarCobro(id: Long, request: RegistrarCobroRequest): IngresoResponse =
        ejecutarLlamada {
            client.post("/api/ingresos/$id/cobro") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun revertirCobro(id: Long): IngresoResponse =
        ejecutarLlamada { client.delete("/api/ingresos/$id/cobro") }
}
