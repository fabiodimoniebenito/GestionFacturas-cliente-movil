package com.fabio.gestionfacturas.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** [DESCONOCIDO] is the fallback for a value this client does not know; it never comes from the backend. */
enum class EstadoCobro { PENDIENTE, COBRADA, DESCONOCIDO }

data class Ingreso(
    val id: Long,
    val usuarioId: Long?,
    val clienteId: Long?,
    val concepto: String?,
    val fechaEmision: LocalDate?,
    val baseImponible: Importe?,
    val iva: Importe?,
    val total: Importe?,
    val moneda: String?,
    val estadoCobro: EstadoCobro?,
    val fechaCobro: LocalDate?,
    val creadoEn: LocalDateTime?,
)

data class NuevoIngreso(
    val total: Importe,
    val clienteId: Long? = null,
    val concepto: String? = null,
    val fechaEmision: LocalDate? = null,
    val baseImponible: Importe? = null,
    val iva: Importe? = null,
    val moneda: String? = null,
)
