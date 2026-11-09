package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.model.ArchivoExportado
import com.fabio.gestionfacturas.domain.repository.ExportRepository
import com.fabio.gestionfacturas.domain.repository.FileSaver
import com.fabio.gestionfacturas.domain.repository.ResultadoGuardado
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

class FakeExportRepository : ExportRepository {
    var resultado: Result<ArchivoExportado> =
        Result.success(ArchivoExportado("informe.xlsx", byteArrayOf(1, 2, 3), "application/test"))
    var puerta: CompletableDeferred<Unit>? = null
    val llamadas = mutableListOf<Pair<LocalDate, LocalDate>>()

    override suspend fun exportarExcel(desde: LocalDate, hasta: LocalDate): Result<ArchivoExportado> {
        llamadas.add(desde to hasta)
        puerta?.await()
        return resultado
    }
}

class FakeFileSaver : FileSaver {
    var resultado: ResultadoGuardado = ResultadoGuardado.Guardado("/tmp/informe.xlsx")
    val guardados = mutableListOf<Triple<String, String, ByteArray>>()

    override suspend fun guardar(nombreSugerido: String, mime: String, bytes: ByteArray): ResultadoGuardado {
        guardados.add(Triple(nombreSugerido, mime, bytes))
        return resultado
    }
}
