package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.PendientesCobro
import com.fabio.gestionfacturas.domain.model.RentabilidadCliente
import com.fabio.gestionfacturas.domain.model.ResumenPeriodo
import kotlinx.datetime.LocalDate

/**
 * Read-only dashboard figures, all computed by the backend. Failures are [ErrorAuth] values:
 * 400 (invalid range, or a field map) is `Validacion`, anything unreadable is `Inesperado`.
 */
interface DashboardRepository {

    /** Totals for the period [desde]..[hasta] (both required). */
    suspend fun resumen(desde: LocalDate, hasta: LocalDate): Result<ResumenPeriodo>

    /** One entry per cliente for the period, including the "sin cliente" one (null `clienteId`). */
    suspend fun rentabilidad(desde: LocalDate, hasta: LocalDate): Result<List<RentabilidadCliente>>

    /** The ingresos still to be collected, with their total. No period. */
    suspend fun pendientesCobro(): Result<PendientesCobro>
}
