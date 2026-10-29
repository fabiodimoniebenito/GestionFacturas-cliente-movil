package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.dto.CategoriaResponse
import com.fabio.gestionfacturas.data.remote.dto.CrearGastoRequest
import com.fabio.gestionfacturas.data.remote.dto.EditarGastoRequest
import com.fabio.gestionfacturas.data.remote.dto.GastoResponse
import com.fabio.gestionfacturas.domain.model.Categoria
import com.fabio.gestionfacturas.domain.model.DatosGasto
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.NuevoGasto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** An `estado` or date this client cannot read is an unexpected response, not a crash. */
internal fun GastoResponse.aModelo(): Gasto = Gasto(
    id = id,
    usuarioId = usuarioId,
    categoriaId = categoriaId,
    clienteId = clienteId,
    emisor = emisor,
    fechaEmision = fechaEmision?.let { LocalDate.parse(it) },
    baseImponible = baseImponible,
    iva = iva,
    moneda = moneda,
    deducible = deducible,
    estado = estado?.let { texto ->
        EstadoGasto.entries.firstOrNull { it.name == texto } ?: throw ErrorAuth.Inesperado
    },
    creadoEn = creadoEn?.let { fechaHoraSinZona(it) },
)

/** `creadoEn` may or may not carry a zone/offset; the zone is dropped, the local date-time kept. */
private fun fechaHoraSinZona(texto: String): LocalDateTime {
    val t = texto.indexOf('T')
    var fin = texto.length
    if (t >= 0) {
        for (i in t until texto.length) {
            if (texto[i] == 'Z' || texto[i] == '+' || (i > t && texto[i] == '-')) {
                fin = i
                break
            }
        }
    }
    return LocalDateTime.parse(texto.substring(0, fin))
}

internal fun NuevoGasto.aRequest() = CrearGastoRequest(
    total = total,
    emisor = emisor,
    fechaEmision = fechaEmision?.toString(),
    baseImponible = baseImponible,
    iva = iva,
    moneda = moneda,
    clienteId = clienteId,
)

internal fun DatosGasto.aRequest() = EditarGastoRequest(
    total = total,
    emisor = emisor,
    fechaEmision = fechaEmision?.toString(),
    baseImponible = baseImponible,
    iva = iva,
    moneda = moneda,
    clienteId = clienteId,
    categoriaId = categoriaId,
    deducible = deducible,
)

internal fun CategoriaResponse.aModelo() = Categoria(id, nombre, deduciblePorDefecto)
