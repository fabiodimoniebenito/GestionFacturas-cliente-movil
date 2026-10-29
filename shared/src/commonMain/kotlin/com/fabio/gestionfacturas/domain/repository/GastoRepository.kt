package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.DatosGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.NuevoGasto

/**
 * Failures are [com.fabio.gestionfacturas.domain.model.ErrorAuth]: 400 = Validacion (field map),
 * 404 = NoEncontradoConDetalle (the gasto, the category or the cliente), 409 on editar/revisar =
 * ConflictoConDetalle (state conflict).
 */
interface GastoRepository {
    suspend fun listar(): Result<List<Gasto>>
    suspend fun obtener(id: Long): Result<Gasto>
    suspend fun crear(nuevo: NuevoGasto): Result<Gasto>
    suspend fun editar(id: Long, datos: DatosGasto): Result<Gasto>
    suspend fun revisar(id: Long): Result<Gasto>
}
