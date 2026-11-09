package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.ArchivoExportado
import kotlinx.datetime.LocalDate

interface ExportRepository {
    suspend fun exportarExcel(desde: LocalDate, hasta: LocalDate): Result<ArchivoExportado>
}
