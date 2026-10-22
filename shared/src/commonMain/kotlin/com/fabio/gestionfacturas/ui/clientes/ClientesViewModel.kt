package com.fabio.gestionfacturas.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClientesViewModel(
    private val repositorio: ClienteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { recargar() }
    }

    fun onRefrescar() {
        if (estaCargando()) {
            return
        }
        viewModelScope.launch { recargar() }
    }

    fun onPedirDesactivar(cliente: Cliente) {
        if (_uiState.value.desactivando) {
            return
        }
        _uiState.update { it.copy(clienteADesactivar = cliente) }
    }

    fun onCancelarDesactivar() {
        if (_uiState.value.desactivando) {
            return
        }
        _uiState.update { it.copy(clienteADesactivar = null) }
    }

    fun onConfirmarDesactivar() {
        val estado = _uiState.value
        val cliente = estado.clienteADesactivar
        if (cliente == null || estado.desactivando) {
            return
        }
        _uiState.update { it.copy(desactivando = true) }
        viewModelScope.launch {
            val resultado = repositorio.desactivar(cliente.id)
            _uiState.update { it.copy(desactivando = false, clienteADesactivar = null) }
            resultado.fold(
                onSuccess = { recargar() },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    if (error == ErrorAuth.NoEncontrado) {
                        _uiState.update { it.copy(mensaje = MensajeClientes.YaNoExiste) }
                        recargar()
                    } else {
                        _uiState.update { it.copy(mensaje = MensajeClientes.DesactivarFallido(error)) }
                    }
                },
            )
        }
    }

    fun onMensajeConsumido() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun estaCargando(): Boolean {
        val estado = _uiState.value
        return estado.carga is CargaClientes.Cargando || estado.refrescando
    }

    /** Loads the list; if one is already on screen it stays visible while the new one arrives. */
    private suspend fun recargar() {
        val habiaLista = _uiState.value.carga is CargaClientes.Contenido
        if (habiaLista) {
            _uiState.update { it.copy(refrescando = true) }
        } else {
            _uiState.update { it.copy(carga = CargaClientes.Cargando) }
        }
        repositorio.listar().fold(
            onSuccess = { clientes ->
                val carga = if (clientes.isEmpty()) CargaClientes.Vacio else CargaClientes.Contenido(clientes)
                _uiState.update { it.copy(carga = carga, refrescando = false) }
            },
            onFailure = { fallo ->
                val error = fallo.comoErrorAuth()
                if (habiaLista) {
                    _uiState.update {
                        it.copy(refrescando = false, mensaje = MensajeClientes.ActualizarFallido(error))
                    }
                } else {
                    _uiState.update { it.copy(carga = CargaClientes.Error(error), refrescando = false) }
                }
            },
        )
    }
}
