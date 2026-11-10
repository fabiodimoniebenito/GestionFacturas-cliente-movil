package com.fabio.gestionfacturas.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.ArchivoSeleccionado
import com.fabio.gestionfacturas.domain.model.validarImagen
import com.fabio.gestionfacturas.domain.repository.FileOpener
import com.fabio.gestionfacturas.domain.repository.GastoRepository
import com.fabio.gestionfacturas.domain.repository.ResultadoSeleccion
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Picks an image, checks it locally (`validarImagen`, before any request) and uploads it. A rejected
 * file is never sent; after an upload error the file is kept so the user can retry.
 */
class DigitalizarViewModel(
    private val fileOpener: FileOpener,
    private val gastos: GastoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalizarUiState())
    val uiState: StateFlow<DigitalizarUiState> = _uiState.asStateFlow()

    /** Also used by "Cambiar". A cancelled dialog changes nothing and shows no message. */
    fun onElegirImagen() {
        if (_uiState.value.subiendo) {
            return
        }
        viewModelScope.launch {
            when (val resultado = fileOpener.elegirImagen()) {
                ResultadoSeleccion.Cancelado -> Unit
                is ResultadoSeleccion.Error -> mostrarRechazo(ErrorDigitalizar.Seleccion(resultado.mensaje))
                is ResultadoSeleccion.Seleccionado -> alSeleccionar(resultado.archivo)
            }
        }
    }

    fun onSubir() {
        val estado = _uiState.value
        val archivo = estado.archivo
        val puedeSubir = estado.fase == FaseDigitalizar.SELECCIONADO || estado.fase == FaseDigitalizar.ERROR
        if (archivo == null || !puedeSubir) {
            return
        }
        val rechazo = validarImagen(archivo).comoErrorDigitalizar()
        if (rechazo != null) {
            mostrarRechazo(rechazo)
            return
        }
        _uiState.update { it.copy(fase = FaseDigitalizar.SUBIENDO, error = null) }
        viewModelScope.launch {
            gastos.digitalizar(archivo).fold(
                onSuccess = { gasto ->
                    _uiState.update { it.copy(fase = FaseDigitalizar.SELECCIONADO, gastoCreadoId = gasto.id) }
                },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth().comoErrorDigitalizar()
                    // The file stays selected so "Reintentar" sends the same one.
                    _uiState.update { it.copy(fase = FaseDigitalizar.ERROR, error = error) }
                },
            )
        }
    }

    fun onNavegacionConsumida() {
        _uiState.update { it.copy(gastoCreadoId = null) }
    }

    private fun alSeleccionar(archivo: ArchivoSeleccionado) {
        val rechazo = validarImagen(archivo).comoErrorDigitalizar()
        if (rechazo != null) {
            mostrarRechazo(rechazo)
            return
        }
        _uiState.update { it.copy(fase = FaseDigitalizar.SELECCIONADO, archivo = archivo, error = null) }
    }

    /**
     * A file rejected before uploading. If a valid one was already selected it stays (with the reason
     * shown); otherwise the screen goes back to its first state with the message.
     */
    private fun mostrarRechazo(error: ErrorDigitalizar) {
        _uiState.update {
            val conservado = it.archivo
            if (conservado == null) {
                it.copy(fase = FaseDigitalizar.INICIAL, error = error)
            } else {
                it.copy(fase = FaseDigitalizar.ERROR, error = error)
            }
        }
    }
}
