package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Gasto

sealed interface CargaGastoDetalle {
    data object Cargando : CargaGastoDetalle

    /** [nombreCategoria] is null when the gasto has no category or the name could not be resolved. */
    data class Contenido(val gasto: Gasto, val nombreCategoria: String?) : CargaGastoDetalle

    /** 404: the gasto does not exist (or is not the user's). */
    data object NoEncontrado : CargaGastoDetalle

    data class Error(val error: ErrorAuth) : CargaGastoDetalle
}

data class GastoDetalleUiState(
    val carga: CargaGastoDetalle = CargaGastoDetalle.Cargando,
) {
    private val estado: EstadoGasto?
        get() = (carga as? CargaGastoDetalle.Contenido)?.gasto?.estado

    /** Only a gasto still waiting for review can be reviewed; a reviewed or exported one cannot. */
    val puedeRevisar: Boolean
        get() = estado == EstadoGasto.BORRADOR || estado == EstadoGasto.PROCESADA

    /**
     * The contract does not say whether REVISADA/EXPORTADA can be edited, so editing is offered
     * whenever the gasto is loaded and the backend decides (409) in the edit flow.
     */
    val puedeEditar: Boolean
        get() = carga is CargaGastoDetalle.Contenido
}
