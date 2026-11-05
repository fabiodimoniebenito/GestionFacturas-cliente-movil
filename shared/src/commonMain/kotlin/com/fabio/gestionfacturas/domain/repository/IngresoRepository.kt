package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.domain.model.NuevoIngreso
import kotlinx.datetime.LocalDate

/**
 * Failures are [com.fabio.gestionfacturas.domain.model.ErrorAuth]: 400 = Validacion (field map),
 * 404 = NoEncontradoConDetalle (the ingreso or the cliente; the message tells them apart),
 * 409 on registrarCobro/revertirCobro = ConflictoConDetalle (invalid cobro transition or date;
 * the backend message is kept).
 */
interface IngresoRepository {
    suspend fun listar(): Result<List<Ingreso>>
    suspend fun obtener(id: Long): Result<Ingreso>
    suspend fun crear(nuevo: NuevoIngreso): Result<Ingreso>

    /** [fechaCobro] null lets the backend choose the date. */
    suspend fun registrarCobro(id: Long, fechaCobro: LocalDate? = null): Result<Ingreso>
    suspend fun revertirCobro(id: Long): Result<Ingreso>
}
