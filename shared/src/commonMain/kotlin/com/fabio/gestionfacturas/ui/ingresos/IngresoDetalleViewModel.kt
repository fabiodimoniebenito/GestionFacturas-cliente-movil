package com.fabio.gestionfacturas.ui.ingresos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.domain.repository.IngresoRepository
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Today in the system time zone (kotlinx-datetime 0.8.0: `todayIn` takes a `kotlin.time.Clock`). */
fun fechaDeHoy(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

class IngresoDetalleViewModel(
    private val idIngreso: Long,
    private val ingresos: IngresoRepository,
    private val clientes: ClienteRepository,
    private val hoy: () -> LocalDate = ::fechaDeHoy,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngresoDetalleUiState())
    val uiState: StateFlow<IngresoDetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun onReintentar() {
        if (_uiState.value.carga is CargaIngresoDetalle.Cargando) {
            return
        }
        cargar()
    }

    // ---- registrar cobro ---------------------------------------------------------------------

    fun onPedirRegistrarCobro() {
        if (!_uiState.value.puedeRegistrarCobro) {
            return
        }
        _uiState.update { it.copy(dialogoCobro = DialogoRegistrarCobro(fecha = hoy().toString()), conflicto = null) }
    }

    fun onFechaCobroCambiada(texto: String) {
        _uiState.update { estado ->
            estado.copy(
                dialogoCobro = estado.dialogoCobro?.copy(fecha = texto, fechaInvalida = false, errorServidor = null),
            )
        }
    }

    fun onCancelarRegistrarCobro() {
        if (_uiState.value.operando) {
            return
        }
        _uiState.update { it.copy(dialogoCobro = null) }
    }

    fun onConfirmarRegistrarCobro() {
        val estado = _uiState.value
        val dialogo = estado.dialogoCobro
        if (dialogo == null || estado.operando) {
            return
        }
        val fecha = try {
            LocalDate.parse(dialogo.fecha.trim())
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(dialogoCobro = dialogo.copy(fechaInvalida = true)) }
            return
        }
        _uiState.update { it.copy(operando = true) }
        viewModelScope.launch {
            ingresos.registrarCobro(idIngreso, fecha).fold(
                onSuccess = { exito(MensajeIngresoDetalle.CobroRegistrado) },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    // A 400 keeps the dialog open so the date can be corrected.
                    if (error is ErrorAuth.Validacion) {
                        _uiState.update {
                            it.copy(operando = false, dialogoCobro = dialogo.copy(errorServidor = error))
                        }
                    } else {
                        falloDeOperacion(error)
                    }
                },
            )
        }
    }

    // ---- revertir cobro ----------------------------------------------------------------------

    fun onPedirRevertirCobro() {
        if (!_uiState.value.puedeRevertirCobro) {
            return
        }
        _uiState.update { it.copy(confirmandoReversion = true, conflicto = null) }
    }

    fun onCancelarRevertirCobro() {
        if (_uiState.value.operando) {
            return
        }
        _uiState.update { it.copy(confirmandoReversion = false) }
    }

    fun onConfirmarRevertirCobro() {
        val estado = _uiState.value
        if (!estado.confirmandoReversion || estado.operando) {
            return
        }
        _uiState.update { it.copy(operando = true) }
        viewModelScope.launch {
            ingresos.revertirCobro(idIngreso).fold(
                onSuccess = { exito(MensajeIngresoDetalle.CobroRevertido) },
                onFailure = { falloDeOperacion(it.comoErrorAuth()) },
            )
        }
    }

    fun onMensajeConsumido() {
        _uiState.update { it.copy(mensaje = null) }
    }

    fun onCambiosNotificados() {
        _uiState.update { it.copy(huboCambios = false) }
    }

    // ---- internals ---------------------------------------------------------------------------

    private fun exito(mensaje: MensajeIngresoDetalle) {
        _uiState.update {
            it.copy(
                operando = false,
                dialogoCobro = null,
                confirmandoReversion = false,
                conflicto = null,
                mensaje = mensaje,
                huboCambios = true,
            )
        }
        cargar()
    }

    /** 404 ends the screen, 409 is explained next to the actions, anything else is a message. */
    private fun falloDeOperacion(error: ErrorAuth) {
        _uiState.update { it.copy(operando = false, dialogoCobro = null, confirmandoReversion = false) }
        when (error) {
            ErrorAuth.NoEncontrado, is ErrorAuth.NoEncontradoConDetalle ->
                _uiState.update { it.copy(carga = CargaIngresoDetalle.NoEncontrado) }
            ErrorAuth.Conflicto -> _uiState.update { it.copy(conflicto = ConflictoCobro(null)) }
            is ErrorAuth.ConflictoConDetalle -> _uiState.update { it.copy(conflicto = ConflictoCobro(error.detalle)) }
            else -> _uiState.update { it.copy(mensaje = MensajeIngresoDetalle.OperacionFallida(error)) }
        }
    }

    private fun cargar() {
        _uiState.update { it.copy(carga = CargaIngresoDetalle.Cargando) }
        viewModelScope.launch {
            ingresos.obtener(idIngreso).fold(
                onSuccess = { ingreso ->
                    val nombre = nombreDeCliente(ingreso)
                    _uiState.update { it.copy(carga = CargaIngresoDetalle.Contenido(ingreso, nombre)) }
                },
                onFailure = { fallo ->
                    val carga = when (val error = fallo.comoErrorAuth()) {
                        ErrorAuth.NoEncontrado, is ErrorAuth.NoEncontradoConDetalle -> CargaIngresoDetalle.NoEncontrado
                        else -> CargaIngresoDetalle.Error(error)
                    }
                    _uiState.update { it.copy(carga = carga) }
                },
            )
        }
    }

    /** Best effort: a failed lookup or a missing (deactivated) cliente only means no name. */
    private suspend fun nombreDeCliente(ingreso: Ingreso): String? {
        val id = ingreso.clienteId ?: return null
        return clientes.listar().getOrNull()?.firstOrNull { it.id == id }?.nombre
    }
}
