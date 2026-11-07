package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.DashboardApi
import com.fabio.gestionfacturas.domain.model.PendientesCobro
import com.fabio.gestionfacturas.domain.model.RentabilidadCliente
import com.fabio.gestionfacturas.domain.model.ResumenPeriodo
import com.fabio.gestionfacturas.domain.repository.DashboardRepository
import kotlinx.datetime.LocalDate

class DashboardRepositoryImpl(private val api: DashboardApi) : DashboardRepository {

    override suspend fun resumen(desde: LocalDate, hasta: LocalDate): Result<ResumenPeriodo> =
        resultadoDe { api.resumen(desde, hasta).aModelo() }

    override suspend fun rentabilidad(desde: LocalDate, hasta: LocalDate): Result<List<RentabilidadCliente>> =
        resultadoDe { api.rentabilidad(desde, hasta).map { it.aModelo() } }

    override suspend fun pendientesCobro(): Result<PendientesCobro> =
        resultadoDe { api.pendientesCobro().aModelo() }
}
