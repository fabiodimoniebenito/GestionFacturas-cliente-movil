package com.fabio.gestionfacturas.data.remote.dto

import com.fabio.gestionfacturas.domain.model.Importe
import kotlinx.serialization.Serializable

/** Shapes from docs/api-contract.md. Optional fields default to null and are omitted from the JSON. */
@Serializable
data class CrearGastoRequest(
    @Serializable(with = ImporteSerializer::class) val total: Importe,
    val emisor: String? = null,
    val fechaEmision: String? = null,
    @Serializable(with = ImporteSerializer::class) val baseImponible: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val iva: Importe? = null,
    val moneda: String? = null,
    val clienteId: Long? = null,
)

@Serializable
data class EditarGastoRequest(
    @Serializable(with = ImporteSerializer::class) val total: Importe,
    val emisor: String? = null,
    val fechaEmision: String? = null,
    @Serializable(with = ImporteSerializer::class) val baseImponible: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val iva: Importe? = null,
    val moneda: String? = null,
    val clienteId: Long? = null,
    val categoriaId: Long? = null,
    val deducible: Boolean? = null,
)

/** The response has no `total`. Dates arrive as text and are parsed in the mapper. */
@Serializable
data class GastoResponse(
    val id: Long,
    val usuarioId: Long? = null,
    val categoriaId: Long? = null,
    val clienteId: Long? = null,
    val emisor: String? = null,
    val fechaEmision: String? = null,
    @Serializable(with = ImporteSerializer::class) val baseImponible: Importe? = null,
    @Serializable(with = ImporteSerializer::class) val iva: Importe? = null,
    val moneda: String? = null,
    val deducible: Boolean? = null,
    val estado: String? = null,
    val creadoEn: String? = null,
)
