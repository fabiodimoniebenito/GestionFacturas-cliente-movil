package com.fabio.gestionfacturas.ui.ingresos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.domain.repository.IngresoRepository
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IngresosViewModel(
    private val ingresos: IngresoRepository,
    private val clientes: ClienteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngresosUiState())
    val uiState: StateFlow<IngresosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { recargar() }
    }

    fun onRefrescar() {
        if (estaCargando()) {
            return
        }
        viewModelScope.launch { recargar() }
    }

    fun onFiltroCambiado(filtro: FiltroIngresos) {
        _uiState.update { it.copy(filtro = filtro) }
    }

    /** The detail closed: reload after a change, or explain that the ingreso is gone. */
    fun onVolverDelDetalle(resultado: ResultadoFormulario) {
        if (resultado == ResultadoFormulario.NO_ENCONTRADO) {
            _uiState.update { it.copy(mensaje = MensajeIngresos.YaNoExiste) }
        }
        onRefrescar()
    }

    fun onMensajeConsumido() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun estaCargando(): Boolean {
        val estado = _uiState.value
        return estado.carga is CargaIngresos.Cargando || estado.refrescando
    }

    /** Loads the list; if one is already on screen it stays visible while the new one arrives. */
    private suspend fun recargar() {
        val habiaLista = _uiState.value.carga is CargaIngresos.Contenido
        if (habiaLista) {
            _uiState.update { it.copy(refrescando = true) }
        } else {
            _uiState.update { it.copy(carga = CargaIngresos.Cargando) }
        }
        ingresos.listar().fold(
            onSuccess = { lista ->
                val carga = if (lista.isEmpty()) CargaIngresos.Vacio else CargaIngresos.Contenido(conNombres(lista))
                _uiState.update { it.copy(carga = carga, refrescando = false) }
            },
            onFailure = { fallo ->
                val error = fallo.comoErrorAuth()
                if (habiaLista) {
                    _uiState.update {
                        it.copy(refrescando = false, mensaje = MensajeIngresos.ActualizarFallido(error))
                    }
                } else {
                    _uiState.update { it.copy(carga = CargaIngresos.Error(error), refrescando = false) }
                }
            },
        )
    }

    /**
     * The response carries only `clienteId`, so names come from the clientes list (best effort: if it
     * fails, or a cliente is missing from it, the row gets no name and the screen shows a placeholder).
     */
    private suspend fun conNombres(lista: List<Ingreso>): List<FilaIngreso> {
        val nombres = clientes.listar().getOrNull().orEmpty().associate { it.id to it.nombre }
        return lista.map { FilaIngreso(it, it.clienteId?.let { id -> nombres[id] }) }
    }
}
