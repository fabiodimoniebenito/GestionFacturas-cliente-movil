package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.remote.dto.CrearGastoRequest
import com.fabio.gestionfacturas.data.remote.dto.EditarGastoRequest
import com.fabio.gestionfacturas.data.remote.dto.GastoResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

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
}
