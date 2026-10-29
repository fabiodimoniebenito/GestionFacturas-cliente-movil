package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Gasto

sealed interface CargaGastos {
    data object Cargando : CargaGastos
    data object Vacio : CargaGastos
    data class Contenido(val gastos: List<Gasto>) : CargaGastos
    data class Error(val error: ErrorAuth) : CargaGastos
}

/** One-shot messages: the screen shows one and tells the ViewModel to clear it. */
sealed interface MensajeGastos {
    /** The form or the detail found that the gasto no longer exists. */
    data object YaNoExiste : MensajeGastos

    /** A reload failed while a list was already on screen, so the old list is kept. */
    data class ActualizarFallido(val error: ErrorAuth) : MensajeGastos
}

data class GastosUiState(
    val carga: CargaGastos = CargaGastos.Cargando,
    /** A reload is running while the previous list stays visible. */
    val refrescando: Boolean = false,
    val mensaje: MensajeGastos? = null,
)
