package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.ClienteApi
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.DatosCliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.NuevoCliente
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import kotlinx.coroutines.CancellationException

class ClienteRepositoryImpl(
    private val api: ClienteApi,
) : ClienteRepository {

    override suspend fun listar(): Result<List<Cliente>> =
        resultado { api.listar().map { it.aModelo() } }

    override suspend fun obtener(id: Long): Result<Cliente> =
        resultado { api.obtener(id).aModelo() }

    override suspend fun crear(nuevo: NuevoCliente): Result<Cliente> =
        resultado { api.crear(nuevo.aRequest()).aModelo() }

    override suspend fun editar(id: Long, datos: DatosCliente): Result<Cliente> =
        resultado { api.editar(id, datos.aRequest()).aModelo() }

    override suspend fun desactivar(id: Long): Result<Unit> =
        resultado { api.desactivar(id) }

    private suspend inline fun <T> resultado(bloque: () -> T): Result<T> {
        try {
            return Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorAuth) {
            return Result.failure(e)
        } catch (e: Throwable) {
            return Result.failure(ErrorAuth.Inesperado)
        }
    }
}
