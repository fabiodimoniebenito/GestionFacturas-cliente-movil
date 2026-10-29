package com.fabio.gestionfacturas.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.repository.CategoriaRepository
import com.fabio.gestionfacturas.domain.repository.GastoRepository
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GastoDetalleViewModel(
    private val idGasto: Long,
    private val gastos: GastoRepository,
    private val categorias: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastoDetalleUiState())
    val uiState: StateFlow<GastoDetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    /** The edit form saved: show the new data. */
    fun onRecargar() {
        cargar()
    }

    fun onReintentar() {
        if (_uiState.value.carga is CargaGastoDetalle.Cargando) {
            return
        }
        cargar()
    }

    fun onPedirRevisar() {
        val estado = _uiState.value
        if (!estado.puedeRevisar || estado.revisando) {
            return
        }
        _uiState.update { it.copy(confirmandoRevision = true) }
    }

    fun onCancelarRevisar() {
        if (_uiState.value.revisando) {
            return
        }
        _uiState.update { it.copy(confirmandoRevision = false) }
    }

    fun onConfirmarRevisar() {
        val estado = _uiState.value
        if (!estado.confirmandoRevision || estado.revisando) {
            return
        }
        _uiState.update { it.copy(revisando = true) }
        viewModelScope.launch {
            gastos.revisar(idGasto).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            revisando = false,
                            confirmandoRevision = false,
                            mensaje = MensajeGastoDetalle.Revisado,
                            huboCambios = true,
                        )
                    }
                    cargar()
                },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    _uiState.update { it.copy(revisando = false, confirmandoRevision = false) }
                    if (error == ErrorAuth.NoEncontrado || error is ErrorAuth.NoEncontradoConDetalle) {
                        _uiState.update { it.copy(carga = CargaGastoDetalle.NoEncontrado) }
                    } else {
                        _uiState.update { it.copy(mensaje = MensajeGastoDetalle.RevisionFallida(error)) }
                    }
                },
            )
        }
    }

    fun onMensajeConsumido() {
        _uiState.update { it.copy(mensaje = null) }
    }

    fun onCambiosNotificados() {
        _uiState.update { it.copy(huboCambios = false) }
    }

    private fun cargar() {
        _uiState.update { it.copy(carga = CargaGastoDetalle.Cargando) }
        viewModelScope.launch {
            gastos.obtener(idGasto).fold(
                onSuccess = { gasto ->
                    val nombre = nombreDeCategoria(gasto.categoriaId)
                    _uiState.update { it.copy(carga = CargaGastoDetalle.Contenido(gasto, nombre)) }
                },
                onFailure = { fallo ->
                    val carga = when (val error = fallo.comoErrorAuth()) {
                        ErrorAuth.NoEncontrado, is ErrorAuth.NoEncontradoConDetalle -> CargaGastoDetalle.NoEncontrado
                        else -> CargaGastoDetalle.Error(error)
                    }
                    _uiState.update { it.copy(carga = carga) }
                },
            )
        }
    }

    /** Best effort: a failed lookup only means the screen shows the id instead of the name. */
    private suspend fun nombreDeCategoria(id: Long?): String? {
        if (id == null) {
            return null
        }
        return categorias.listar().getOrNull()?.firstOrNull { it.id == id }?.nombre
    }
}
