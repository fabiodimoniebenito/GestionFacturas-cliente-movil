package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.IngresoPendiente
import com.fabio.gestionfacturas.domain.model.PendientesCobro
import com.fabio.gestionfacturas.domain.model.RentabilidadCliente
import com.fabio.gestionfacturas.domain.model.ResumenPeriodo
import com.fabio.gestionfacturas.domain.repository.DashboardRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

/** Ten DISTINCT amounts (1.00 .. 10.00) in the order of the API contract, to catch crossed fields. */
fun resumenDistinto() = ResumenPeriodo(
    facturadoTotal = Importe("1.00"),
    facturadoBase = Importe("2.00"),
    cobradoTotal = Importe("3.00"),
    cobradoBase = Importe("4.00"),
    gastosTotal = Importe("5.00"),
    gastosBase = Importe("6.00"),
    beneficioCajaTotal = Importe("7.00"),
    beneficioCajaBase = Importe("8.00"),
    beneficioFacturadoTotal = Importe("9.00"),
    beneficioFacturadoBase = Importe("10.00"),
)

fun pendienteVacio(id: Long, clienteId: Long? = null) = IngresoPendiente(
    id = id,
    clienteId = clienteId,
    concepto = null,
    fechaEmision = null,
    baseImponible = null,
    iva = null,
    total = null,
)

class FakeDashboardRepository : DashboardRepository {
    var resultadoResumen: Result<ResumenPeriodo> = Result.success(resumenDistinto())
    var resultadoRentabilidad: Result<List<RentabilidadCliente>> = Result.success(emptyList())
    var resultadoPendientes: Result<PendientesCobro> =
        Result.success(PendientesCobro(emptyList(), Importe("0.00")))
    var puertaResumen: CompletableDeferred<Unit>? = null
    var puertaPendientes: CompletableDeferred<Unit>? = null

    var puertaRentabilidad: CompletableDeferred<Unit>? = null

    val llamadasResumen = mutableListOf<Pair<LocalDate, LocalDate>>()
    val llamadasRentabilidad = mutableListOf<Pair<LocalDate, LocalDate>>()
    var llamadasPendientes = 0

    override suspend fun resumen(desde: LocalDate, hasta: LocalDate): Result<ResumenPeriodo> {
        llamadasResumen.add(desde to hasta)
        puertaResumen?.await()
        return resultadoResumen
    }

    override suspend fun rentabilidad(desde: LocalDate, hasta: LocalDate): Result<List<RentabilidadCliente>> {
        llamadasRentabilidad.add(desde to hasta)
        puertaRentabilidad?.await()
        return resultadoRentabilidad
    }

    override suspend fun pendientesCobro(): Result<PendientesCobro> {
        llamadasPendientes++
        puertaPendientes?.await()
        return resultadoPendientes
    }
}
