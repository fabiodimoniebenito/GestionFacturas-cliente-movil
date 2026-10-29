package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.remote.dto.CategoriaResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get

/** `/api/categorias`. Must be given the AUTHENTICATED client. */
class CategoriaApi(private val client: HttpClient) {

    suspend fun listar(): List<CategoriaResponse> =
        ejecutarLlamada { client.get("/api/categorias") }
}
