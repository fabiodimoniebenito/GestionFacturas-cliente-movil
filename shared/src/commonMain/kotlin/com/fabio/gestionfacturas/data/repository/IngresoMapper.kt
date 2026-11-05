package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.dto.CrearIngresoRequest
import com.fabio.gestionfacturas.data.remote.dto.IngresoResponse
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.domain.model.NuevoIngreso
import kotlinx.datetime.LocalDate

/** An unknown `estadoCobro` becomes [EstadoCobro.DESCONOCIDO]; an unreadable date is an unexpected response. */
internal fun IngresoResponse.aModelo(): Ingreso = Ingreso(
    id = id,
    usuarioId = usuarioId,
    clienteId = clienteId,
    concepto = concepto,
    fechaEmision = fechaEmision?.let { LocalDate.parse(it) },
    baseImponible = baseImponible,
    iva = iva,
    total = total,
    moneda = moneda,
    estadoCobro = estadoCobro?.let { texto ->
        EstadoCobro.entries.firstOrNull { it.name == texto && it != EstadoCobro.DESCONOCIDO }
            ?: EstadoCobro.DESCONOCIDO
    },
    fechaCobro = fechaCobro?.let { LocalDate.parse(it) },
    creadoEn = creadoEn?.let { fechaHoraSinZona(it) },
)

internal fun NuevoIngreso.aRequest() = CrearIngresoRequest(
    total = total,
    clienteId = clienteId,
    concepto = concepto,
    fechaEmision = fechaEmision?.toString(),
    baseImponible = baseImponible,
    iva = iva,
    moneda = moneda,
)
