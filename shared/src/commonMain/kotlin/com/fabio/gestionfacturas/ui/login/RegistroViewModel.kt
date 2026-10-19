package com.fabio.gestionfacturas.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.repository.AuthRepository
import com.fabio.gestionfacturas.ui.common.CampoFormulario
import com.fabio.gestionfacturas.ui.common.ErrorCampo
import com.fabio.gestionfacturas.ui.common.ValidacionFormulario
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistroViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) {
        _uiState.update {
            it.copy(email = valor, erroresCampo = it.erroresCampo - CampoFormulario.EMAIL, error = null)
        }
    }

    fun onNombreChange(valor: String) {
        _uiState.update {
            it.copy(nombre = valor, erroresCampo = it.erroresCampo - CampoFormulario.NOMBRE, error = null)
        }
    }

    fun onPasswordChange(valor: String) {
        _uiState.update {
            it.copy(password = valor, erroresCampo = it.erroresCampo - CampoFormulario.PASSWORD, error = null)
        }
    }

    fun onRegistrar() {
        val actual = _uiState.value
        if (actual.cargando) {
            return
        }

        val errores = validar(actual)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(erroresCampo = errores, error = null) }
            return
        }

        _uiState.update { it.copy(cargando = true, erroresCampo = emptyMap(), error = null) }
        viewModelScope.launch {
            val resultado = authRepository.registrar(actual.email.trim(), actual.nombre.trim(), actual.password)
            resultado.fold(
                onSuccess = {
                    _uiState.update { it.copy(cargando = false, registroExitoso = true) }
                },
                onFailure = { fallo ->
                    _uiState.update { it.copy(cargando = false, error = fallo.comoErrorAuth()) }
                },
            )
        }
    }

    /** The screen has navigated: clear the one-shot effect so it is not replayed. */
    fun onNavegacionConsumida() {
        _uiState.update { it.copy(registroExitoso = false) }
    }

    private fun validar(estado: RegistroUiState): Map<CampoFormulario, ErrorCampo> {
        val errores = mutableMapOf<CampoFormulario, ErrorCampo>()
        val errorEmail = ValidacionFormulario.email(estado.email)
        if (errorEmail != null) {
            errores[CampoFormulario.EMAIL] = errorEmail
        }
        val errorNombre = ValidacionFormulario.nombre(estado.nombre)
        if (errorNombre != null) {
            errores[CampoFormulario.NOMBRE] = errorNombre
        }
        val errorPassword = ValidacionFormulario.password(estado.password)
        if (errorPassword != null) {
            errores[CampoFormulario.PASSWORD] = errorPassword
        }
        return errores
    }
}
