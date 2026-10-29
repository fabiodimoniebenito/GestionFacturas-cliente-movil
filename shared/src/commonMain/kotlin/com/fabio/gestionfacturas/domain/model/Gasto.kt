package com.fabio.gestionfacturas.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

enum class EstadoGasto { BORRADOR, PROCESADA, REVISADA, EXPORTADA }

/** The backend's response has no `total`; only base and IVA are returned. */
data class Gasto(
    val id: Long,
    val usuarioId: Long?,
    val categoriaId: Long?,
    val clienteId: Long?,
    val emisor: String?,
    val fechaEmision: LocalDate?,
    val baseImponible: Importe?,
    val iva: Importe?,
    val moneda: String?,
    val deducible: Boolean?,
    val estado: EstadoGasto?,
    val creadoEn: LocalDateTime?,
)

/** Creation payload. Category and `deducible` can only be set by editing. */
data class NuevoGasto(
    val total: Importe,
    val emisor: String? = null,
    val fechaEmision: LocalDate? = null,
    val baseImponible: Importe? = null,
    val iva: Importe? = null,
    val moneda: String? = null,
    val clienteId: Long? = null,
)

data class DatosGasto(
    val total: Importe,
    val emisor: String? = null,
    val fechaEmision: LocalDate? = null,
    val baseImponible: Importe? = null,
    val iva: Importe? = null,
    val moneda: String? = null,
    val clienteId: Long? = null,
    val categoriaId: Long? = null,
    val deducible: Boolean? = null,
)
