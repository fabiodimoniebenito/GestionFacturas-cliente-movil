package com.fabio.gestionfacturas.domain.model

import kotlinx.datetime.LocalDate

/**
 * The ten figures the backend computes for a period. The `beneficio*` ones can be negative.
 * "Total" includes IVA, "Base" does not.
 */
data class ResumenPeriodo(
    val facturadoTotal: Importe,
    val facturadoBase: Importe,
    val cobradoTotal: Importe,
    val cobradoBase: Importe,
    val gastosTotal: Importe,
    val gastosBase: Importe,
    val beneficioCajaTotal: Importe,
    val beneficioCajaBase: Importe,
    val beneficioFacturadoTotal: Importe,
    val beneficioFacturadoBase: Importe,
)

/**
 * Profitability of one cliente. A null [clienteId] is the "sin cliente" entry. The API sends no
 * name, so [clienteNombre] is null as it comes from the data layer; the UI resolves it from the clientes.
 */
data class RentabilidadCliente(
    val clienteId: Long?,
    val clienteNombre: String? = null,
    val resumen: ResumenPeriodo,
)

/** One uncollected ingreso, in the slimmer shape the dashboard returns. */
data class IngresoPendiente(
    val id: Long,
    val clienteId: Long?,
    val concepto: String?,
    val fechaEmision: LocalDate?,
    val baseImponible: Importe?,
    val iva: Importe?,
    val total: Importe?,
)

/** The uncollected ingresos plus their aggregate [total], computed by the backend. */
data class PendientesCobro(
    val ingresos: List<IngresoPendiente>,
    val total: Importe,
)
