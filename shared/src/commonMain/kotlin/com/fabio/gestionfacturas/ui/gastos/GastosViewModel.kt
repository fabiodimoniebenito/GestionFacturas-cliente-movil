package com.fabio.gestionfacturas.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.repository.GastoRepository
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GastosViewModel(
    private val repositorio: GastoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastosUiState())
    val uiState: StateFlow<GastosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { recargar() }
    }

    fun onRefrescar() {
        if (estaCargando()) {
            return
        }
        viewModelScope.launch { recargar() }
    }

    /** The form (or the detail) closed: reload after a save, or explain that the gasto is gone. */
    fun onVolverDelFormulario(resultado: ResultadoFormulario) {
        if (resultado == ResultadoFormulario.NO_ENCONTRADO) {
            _uiState.update { it.copy(mensaje = MensajeGastos.YaNoExiste) }
        }
        onRefrescar()
    }

    fun onMensajeConsumido() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun estaCargando(): Boolean {
        val estado = _uiState.value
        return estado.carga is CargaGastos.Cargando || estado.refrescando
    }

    /** Loads the list; if one is already on screen it stays visible while the new one arrives. */
    private suspend fun recargar() {
        val habiaLista = _uiState.value.carga is CargaGastos.Contenido
        if (habiaLista) {
            _uiState.update { it.copy(refrescando = true) }
        } else {
            _uiState.update { it.copy(carga = CargaGastos.Cargando) }
        }
        repositorio.listar().fold(
            onSuccess = { gastos ->
                val carga = if (gastos.isEmpty()) CargaGastos.Vacio else CargaGastos.Contenido(gastos)
                _uiState.update { it.copy(carga = carga, refrescando = false) }
            },
            onFailure = { fallo ->
                val error = fallo.comoErrorAuth()
                if (habiaLista) {
                    _uiState.update {
                        it.copy(refrescando = false, mensaje = MensajeGastos.ActualizarFallido(error))
                    }
                } else {
                    _uiState.update { it.copy(carga = CargaGastos.Error(error), refrescando = false) }
                }
            },
        )
    }
}
