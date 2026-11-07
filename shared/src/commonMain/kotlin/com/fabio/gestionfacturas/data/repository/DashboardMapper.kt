package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.dto.IngresoPendienteResponse
import com.fabio.gestionfacturas.data.remote.dto.PendientesCobroResponse
import com.fabio.gestionfacturas.data.remote.dto.RentabilidadClienteResponse
import com.fabio.gestionfacturas.data.remote.dto.ResumenPeriodoResponse
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.IngresoPendiente
import com.fabio.gestionfacturas.domain.model.PendientesCobro
import com.fabio.gestionfacturas.domain.model.RentabilidadCliente
import com.fabio.gestionfacturas.domain.model.ResumenPeriodo
import kotlinx.datetime.LocalDate

/**
 * A missing money figure is an unexpected response (it throws, and `resultadoDe` turns that into
 * `Inesperado`): it must never show up as a silent 0.
 */
private fun Importe?.obligatorio(campo: String): Importe =
    this ?: throw IllegalStateException("Falta $campo en la respuesta del dashboard")

internal fun ResumenPeriodoResponse.aModelo(): ResumenPeriodo = ResumenPeriodo(
    facturadoTotal = facturadoTotal.obligatorio("facturadoTotal"),
    facturadoBase = facturadoBase.obligatorio("facturadoBase"),
    cobradoTotal = cobradoTotal.obligatorio("cobradoTotal"),
    cobradoBase = cobradoBase.obligatorio("cobradoBase"),
    gastosTotal = gastosTotal.obligatorio("gastosTotal"),
    gastosBase = gastosBase.obligatorio("gastosBase"),
    beneficioCajaTotal = beneficioCajaTotal.obligatorio("beneficioCajaTotal"),
    beneficioCajaBase = beneficioCajaBase.obligatorio("beneficioCajaBase"),
    beneficioFacturadoTotal = beneficioFacturadoTotal.obligatorio("beneficioFacturadoTotal"),
    beneficioFacturadoBase = beneficioFacturadoBase.obligatorio("beneficioFacturadoBase"),
)

/** The API has no client name, so `clienteNombre` stays null here. */
internal fun RentabilidadClienteResponse.aModelo(): RentabilidadCliente = RentabilidadCliente(
    clienteId = clienteId,
    resumen = (resumen ?: throw IllegalStateException("Falta resumen en la rentabilidad")).aModelo(),
)

internal fun IngresoPendienteResponse.aModelo(): IngresoPendiente = IngresoPendiente(
    id = id,
    clienteId = clienteId,
    concepto = concepto,
    fechaEmision = fechaEmision?.let { LocalDate.parse(it) },
    baseImponible = baseImponible,
    iva = iva,
    total = total,
)

internal fun PendientesCobroResponse.aModelo(): PendientesCobro = PendientesCobro(
    ingresos = ingresos.map { it.aModelo() },
    total = total.obligatorio("total"),
)
