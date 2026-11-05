package com.fabio.gestionfacturas.ui.ingresos

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.domain.model.Ingreso

sealed interface CargaIngresoDetalle {
    data object Cargando : CargaIngresoDetalle

    /** [nombreCliente] is null when the ingreso has no cliente or the cliente is not in the list. */
    data class Contenido(val ingreso: Ingreso, val nombreCliente: String?) : CargaIngresoDetalle

    /** 404: the ingreso (or its cliente) does not exist. The screen goes back to the list. */
    data object NoEncontrado : CargaIngresoDetalle

    data class Error(val error: ErrorAuth) : CargaIngresoDetalle
}

/** The "Registrar cobro" dialog. [fecha] is the raw text of the yyyy-MM-dd field. */
data class DialogoRegistrarCobro(
    val fecha: String,
    val fechaInvalida: Boolean = false,
    /** A 400 from the backend (e.g. a date it rejected). */
    val errorServidor: ErrorAuth? = null,
)

/** The backend's 409 message ([detalle] is null when it sent none), shown next to the actions. */
data class ConflictoCobro(val detalle: String?)

/** One-shot messages: the screen shows one and tells the ViewModel to clear it. */
sealed interface MensajeIngresoDetalle {
    data object CobroRegistrado : MensajeIngresoDetalle
    data object CobroRevertido : MensajeIngresoDetalle
    data class OperacionFallida(val error: ErrorAuth) : MensajeIngresoDetalle
}

data class IngresoDetalleUiState(
    val carga: CargaIngresoDetalle = CargaIngresoDetalle.Cargando,
    val dialogoCobro: DialogoRegistrarCobro? = null,
    val confirmandoReversion: Boolean = false,
    /** A cobro operation is in flight. */
    val operando: Boolean = false,
    val conflicto: ConflictoCobro? = null,
    val mensaje: MensajeIngresoDetalle? = null,
    /** A cobro operation succeeded: the screen tells the list to refresh, then clears this. */
    val huboCambios: Boolean = false,
) {
    private val estadoCobro: EstadoCobro?
        get() = (carga as? CargaIngresoDetalle.Contenido)?.ingreso?.estadoCobro

    val puedeRegistrarCobro: Boolean
        get() = estadoCobro == EstadoCobro.PENDIENTE && !operando

    val puedeRevertirCobro: Boolean
        get() = estadoCobro == EstadoCobro.COBRADA && !operando
}
