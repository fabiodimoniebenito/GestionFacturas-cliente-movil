package com.fabio.gestionfacturas.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.DatosCliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.NuevoCliente
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One form for both modes: [idCliente] null creates, otherwise it loads and edits that cliente. */
class ClienteFormViewModel(
    private val idCliente: Long?,
    private val repositorio: ClienteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ClienteFormUiState(
            idCliente = idCliente,
            carga = if (idCliente == null) CargaFormCliente.Lista else CargaFormCliente.Cargando,
        ),
    )
    val uiState: StateFlow<ClienteFormUiState> = _uiState.asStateFlow()

    init {
        if (idCliente != null) {
            cargar(idCliente)
        }
    }

    fun onReintentarCarga() {
        if (idCliente == null || _uiState.value.carga !is CargaFormCliente.Error) {
            return
        }
        cargar(idCliente)
    }

    fun onNombreChange(valor: String) = editarCampo(CampoCliente.NOMBRE) { it.copy(nombre = valor) }

    fun onNifChange(valor: String) = editarCampo(CampoCliente.NIF) { it.copy(nif = valor) }

    fun onEmailChange(valor: String) = editarCampo(CampoCliente.EMAIL) { it.copy(email = valor) }

    fun onTelefonoChange(valor: String) = editarCampo(CampoCliente.TELEFONO) { it.copy(telefono = valor) }

    fun onGuardar() {
        val estado = _uiState.value
        if (estado.guardando || estado.carga != CargaFormCliente.Lista) {
            return
        }
        val nombre = estado.nombre.trim()
        val nif = estado.nif.trim()
        val errores = buildMap {
            if (nombre.isEmpty()) {
                put(CampoCliente.NOMBRE, ErrorCampoCliente.NOMBRE_VACIO)
            }
            // The NIF cannot be edited, so it is only required on create.
            if (idCliente == null && nif.isEmpty()) {
                put(CampoCliente.NIF, ErrorCampoCliente.NIF_VACIO)
            }
        }
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(erroresLocales = errores, error = null) }
            return
        }
        val email = estado.email.trim().ifEmpty { null }
        val telefono = estado.telefono.trim().ifEmpty { null }

        _uiState.update { it.copy(guardando = true, erroresLocales = emptyMap(), error = null) }
        viewModelScope.launch {
            val resultado = if (idCliente == null) {
                repositorio.crear(NuevoCliente(nombre = nombre, nif = nif, email = email, telefono = telefono))
            } else {
                repositorio.editar(idCliente, DatosCliente(nombre = nombre, email = email, telefono = telefono))
            }
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, salida = ResultadoFormulario.GUARDADO) } },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    if (idCliente != null && error == ErrorAuth.NoEncontrado) {
                        _uiState.update { it.copy(guardando = false, salida = ResultadoFormulario.NO_ENCONTRADO) }
                    } else {
                        _uiState.update { it.copy(guardando = false, error = error) }
                    }
                },
            )
        }
    }

    fun onSalidaConsumida() {
        _uiState.update { it.copy(salida = null) }
    }

    private fun cargar(id: Long) {
        _uiState.update { it.copy(carga = CargaFormCliente.Cargando) }
        viewModelScope.launch {
            repositorio.obtener(id).fold(
                onSuccess = { cliente -> _uiState.update { it.conDatosDe(cliente) } },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    if (error == ErrorAuth.NoEncontrado) {
                        _uiState.update { it.copy(salida = ResultadoFormulario.NO_ENCONTRADO) }
                    } else {
                        _uiState.update { it.copy(carga = CargaFormCliente.Error(error)) }
                    }
                },
            )
        }
    }

    /** Editing a field clears its own local error and the general error. */
    private fun editarCampo(campo: CampoCliente, cambio: (ClienteFormUiState) -> ClienteFormUiState) {
        _uiState.update { cambio(it).copy(erroresLocales = it.erroresLocales - campo, error = null) }
    }

    private fun ClienteFormUiState.conDatosDe(cliente: Cliente) = copy(
        carga = CargaFormCliente.Lista,
        nombre = cliente.nombre,
        nif = cliente.nif,
        email = cliente.email.orEmpty(),
        telefono = cliente.telefono.orEmpty(),
    )
}
