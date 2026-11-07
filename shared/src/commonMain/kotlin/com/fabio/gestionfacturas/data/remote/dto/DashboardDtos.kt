package com.fabio.gestionfacturas.data.remote.dto

import com.fabio.gestionfacturas.domain.model.Importe
import kotlinx.serialization.Serializable

/** Shapes from docs/api-contract.md. No field is marked required there, so all are nullable or defaulted. */
@Serializable
data class ResumenPeriodoResponse(
    @Serializable(with = ImporteSerializer::class) val facturadoTotal: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val facturadoBase: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val cobradoTotal: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val cobradoBase: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val gastosTotal: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val gastosBase: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val beneficioCajaTotal: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val beneficioCajaBase: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val beneficioFacturadoTotal: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val beneficioFacturadoBase: Importe? = null,
)

/** There is no client name in the contract, only the id (null = the "sin cliente" entry). */
@Serializable
data class RentabilidadClienteResponse(
    val clienteId: Long? = null,
    val resumen: ResumenPeriodoResponse? = null,
)

@Serializable
data class IngresoPendienteResponse(
    val id: Long,
    val clienteId: Long? = null,
    val concepto: String? = null,
    val fechaEmision: String? = null,
    @Serializable(with = ImporteSerializer::class) val baseImponible: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val iva: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val total: Importe? = null,
)

@Serializable
data class PendientesCobroResponse(
    val ingresos: List<IngresoPendienteResponse> = emptyList(),
    @Serializable(with = ImporteSerializer::class) val total: Importe? = null,
)
