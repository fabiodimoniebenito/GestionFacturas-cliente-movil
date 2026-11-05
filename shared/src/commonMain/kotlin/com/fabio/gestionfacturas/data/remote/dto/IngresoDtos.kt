package com.fabio.gestionfacturas.data.remote.dto

import com.fabio.gestionfacturas.domain.model.Importe
import kotlinx.serialization.Serializable

/** Shapes from docs/api-contract.md. Optional fields default to null and are omitted from the JSON. */
@Serializable
data class CrearIngresoRequest(
    @Serializable(with = ImporteSerializer::class) val total: Importe,
    val clienteId: Long? = null,
    val concepto: String? = null,
    val fechaEmision: String? = null,
    @Serializable(with = ImporteSerializer::class) val baseImponible: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val iva: Importe? = null,
    val moneda: String? = null,
)

/** The body is required by the backend: without a date it is sent as `{}`. */
@Serializable
data class RegistrarCobroRequest(
    val fechaCobro: String? = null,
)

/** Dates and `estadoCobro` arrive as text and are parsed in the mapper. */
@Serializable
data class IngresoResponse(
    val id: Long,
    val usuarioId: Long? = null,
    val clienteId: Long? = null,
    val concepto: String? = null,
    val fechaEmision: String? = null,
    @Serializable(with = ImporteSerializer::class) val baseImponible: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val iva: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val total: Importe? = null,
    val moneda: String? = null,
    val estadoCobro: String? = null,
    val fechaCobro: String? = null,
    val creadoEn: String? = null,
)
