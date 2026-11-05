package com.fabio.gestionfacturas.ui.ingresos

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.domain.model.Ingreso

/** Client-side filter on `estadoCobro`: the list endpoint has no filter parameter. */
enum class FiltroIngresos { TODOS, PENDIENTES, COBRADOS }

/** [nombreCliente] is null when the ingreso has no cliente or the cliente is not in the loaded list. */
data class FilaIngreso(val ingreso: Ingreso, val nombreCliente: String?)

sealed interface CargaIngresos {
    data object Cargando : CargaIngresos
    data object Vacio : CargaIngresos
    data class Contenido(val filas: List<FilaIngreso>) : CargaIngresos
    data class Error(val error: ErrorAuth) : CargaIngresos
}

/** One-shot messages: the screen shows one and tells the ViewModel to clear it. */
sealed interface MensajeIngresos {
    /** The detail found that the ingreso no longer exists. */
    data object YaNoExiste : MensajeIngresos

    /** A reload failed while a list was already on screen, so the old list is kept. */
    data class ActualizarFallido(val error: ErrorAuth) : MensajeIngresos
}

data class IngresosUiState(
    val carga: CargaIngresos = CargaIngresos.Cargando,
    val filtro: FiltroIngresos = FiltroIngresos.TODOS,
    /** A reload is running while the previous list stays visible. */
    val refrescando: Boolean = false,
    val mensaje: MensajeIngresos? = null,
) {
    /** The rows that pass [filtro]. Empty unless the list is loaded. */
    val visibles: List<FilaIngreso>
        get() {
            val filas = (carga as? CargaIngresos.Contenido)?.filas ?: return emptyList()
            return when (filtro) {
                FiltroIngresos.TODOS -> filas
                FiltroIngresos.PENDIENTES -> filas.filter { it.ingreso.estadoCobro == EstadoCobro.PENDIENTE }
                FiltroIngresos.COBRADOS -> filas.filter { it.ingreso.estadoCobro == EstadoCobro.COBRADA }
            }
        }
}
