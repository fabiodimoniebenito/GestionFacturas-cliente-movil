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

    private fun cargar() {
        _uiState.value = GastoDetalleUiState(CargaGastoDetalle.Cargando)
        viewModelScope.launch {
            gastos.obtener(idGasto).fold(
                onSuccess = { gasto ->
                    _uiState.value = GastoDetalleUiState(
                        CargaGastoDetalle.Contenido(gasto, nombreDeCategoria(gasto.categoriaId)),
                    )
                },
                onFailure = { fallo ->
                    val carga = when (val error = fallo.comoErrorAuth()) {
                        ErrorAuth.NoEncontrado, is ErrorAuth.NoEncontradoConDetalle -> CargaGastoDetalle.NoEncontrado
                        else -> CargaGastoDetalle.Error(error)
                    }
                    _uiState.value = GastoDetalleUiState(carga)
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
