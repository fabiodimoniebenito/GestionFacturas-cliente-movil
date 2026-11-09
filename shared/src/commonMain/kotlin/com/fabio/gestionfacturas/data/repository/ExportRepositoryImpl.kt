package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.ExportApi
import com.fabio.gestionfacturas.domain.model.ArchivoExportado
import com.fabio.gestionfacturas.domain.repository.ExportRepository
import kotlinx.datetime.LocalDate

class ExportRepositoryImpl(private val api: ExportApi) : ExportRepository {

    override suspend fun exportarExcel(desde: LocalDate, hasta: LocalDate): Result<ArchivoExportado> =
        resultadoDe { api.excel(desde, hasta) }
}
