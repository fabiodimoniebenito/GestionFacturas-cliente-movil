package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.model.DatosGasto
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.NuevoGasto
import com.fabio.gestionfacturas.domain.repository.GastoRepository
import kotlinx.coroutines.CompletableDeferred

/** A gasto with every optional field null, as the OCR can leave it. */
fun gastoVacio(id: Long, estado: EstadoGasto? = null) = Gasto(
    id = id,
    usuarioId = null,
    categoriaId = null,
    clienteId = null,
    emisor = null,
    fechaEmision = null,
    baseImponible = null,
    iva = null,
    moneda = null,
    deducible = null,
    estado = estado,
    creadoEn = null,
)

class FakeGastoRepository : GastoRepository {
    var resultadoListar: Result<List<Gasto>> = Result.success(emptyList())
    var resultadoObtener: Result<Gasto> = Result.failure(ErrorAuth.Inesperado)
    var resultadoCrear: Result<Gasto> = Result.failure(ErrorAuth.Inesperado)
    var resultadoEditar: Result<Gasto> = Result.failure(ErrorAuth.Inesperado)
    var resultadoRevisar: Result<Gasto> = Result.failure(ErrorAuth.Inesperado)
    var puertaListar: CompletableDeferred<Unit>? = null
    var puertaObtener: CompletableDeferred<Unit>? = null

    var llamadasListar = 0
    val obtenidos = mutableListOf<Long>()

    override suspend fun listar(): Result<List<Gasto>> {
        llamadasListar++
        puertaListar?.await()
        return resultadoListar
    }

    override suspend fun obtener(id: Long): Result<Gasto> {
        obtenidos.add(id)
        puertaObtener?.await()
        return resultadoObtener
    }

    override suspend fun crear(nuevo: NuevoGasto): Result<Gasto> = resultadoCrear

    override suspend fun editar(id: Long, datos: DatosGasto): Result<Gasto> = resultadoEditar

    override suspend fun revisar(id: Long): Result<Gasto> = resultadoRevisar
}
