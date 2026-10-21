package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.DatosCliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.NuevoCliente

/** Clientes of the logged-in user. Failures are reported as [ErrorAuth] inside the [Result]. */
interface ClienteRepository {

    /** Exactly what the backend returns: inactive clientes are not filtered out or in by the client. */
    suspend fun listar(): Result<List<Cliente>>

    suspend fun obtener(id: Long): Result<Cliente>

    suspend fun crear(nuevo: NuevoCliente): Result<Cliente>

    suspend fun editar(id: Long, datos: DatosCliente): Result<Cliente>

    /** Deactivates the cliente (the backend does not delete it). */
    suspend fun desactivar(id: Long): Result<Unit>
}
