package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.DatosCliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.NuevoCliente
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * ClienteRepository for ViewModel tests: no network, programmable results. Set [puertaListar] or
 * [puertaDesactivar] to keep that call suspended so the test can observe the loading state.
 */
class FakeClienteRepository : ClienteRepository {

    var resultadoListar: Result<List<Cliente>> = Result.success(emptyList())
    var resultadoDesactivar: Result<Unit> = Result.success(Unit)
    var puertaListar: CompletableDeferred<Unit>? = null
    var puertaDesactivar: CompletableDeferred<Unit>? = null

    var llamadasListar = 0
    val desactivados = mutableListOf<Long>()

    override suspend fun listar(): Result<List<Cliente>> {
        llamadasListar++
        puertaListar?.await()
        return resultadoListar
    }

    override suspend fun desactivar(id: Long): Result<Unit> {
        desactivados.add(id)
        puertaDesactivar?.await()
        return resultadoDesactivar
    }

    // Not used by the list screen.
    override suspend fun obtener(id: Long): Result<Cliente> = Result.failure(ErrorAuth.Inesperado)

    override suspend fun crear(nuevo: NuevoCliente): Result<Cliente> = Result.failure(ErrorAuth.Inesperado)

    override suspend fun editar(id: Long, datos: DatosCliente): Result<Cliente> =
        Result.failure(ErrorAuth.Inesperado)
}
