package com.fabio.gestionfacturas.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.PendientesCobro
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.domain.repository.DashboardRepository
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import com.fabio.gestionfacturas.ui.ingresos.fechaDeHoy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * The resumen and the pendientes de cobro load independently: each has its own state, so a failure
 * in one never blanks the other. Nothing retries by itself (a 429 must not start a request loop).
 * [hoy] is injectable so tests get deterministic presets.
 */
class DashboardViewModel(
    private val dashboard: DashboardRepository,
    private val clientes: ClienteRepository,
    private val hoy: () -> LocalDate = ::fechaDeHoy,
) : ViewModel() {

    private val _uiState = MutableStateFlow(estadoInicial())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var trabajoResumen: Job? = null
    private var trabajoPendientes: Job? = null

    init {
        cargarResumen(conservarContenido = false)
        cargarPendientes(conservarContenido = false)
    }

    private fun estadoInicial(): DashboardUiState {
        val preset = PresetPeriodo.TRIMESTRE_ACTUAL
        val periodo = checkNotNull(periodoDePreset(preset, hoy()))
        return DashboardUiState(
            preset = preset,
            periodo = periodo,
            desdeTexto = periodo.desde.toString(),
            hastaTexto = periodo.hasta.toString(),
        )
    }

    // ---- period and mode ---------------------------------------------------------------------

    /** A fixed preset loads right away; "Personalizado" only shows the fields and waits for "Aplicar". */
    fun onPresetSeleccionado(preset: PresetPeriodo) {
        if (preset == _uiState.value.preset) {
            return
        }
        val periodo = periodoDePreset(preset, hoy())
        if (periodo == null) {
            _uiState.update {
                it.copy(
                    preset = preset,
                    desdeTexto = it.periodo.desde.toString(),
                    hastaTexto = it.periodo.hasta.toString(),
                    errorPersonalizado = null,
                )
            }
            return
        }
        _uiState.update { it.copy(preset = preset, periodo = periodo, errorPersonalizado = null) }
        cargarResumen(conservarContenido = false)
    }

    fun onDesdeCambiado(texto: String) {
        _uiState.update { it.copy(desdeTexto = texto, errorPersonalizado = null) }
    }

    fun onHastaCambiado(texto: String) {
        _uiState.update { it.copy(hastaTexto = texto, errorPersonalizado = null) }
    }

    /** Validates the typed range on the client; an invalid one never reaches the backend. */
    fun onAplicarPersonalizado() {
        val estado = _uiState.value
        if (estado.preset != PresetPeriodo.PERSONALIZADO) {
            return
        }
        val desde = parsearFecha(estado.desdeTexto)
        val hasta = parsearFecha(estado.hastaTexto)
        val error = when {
            desde == null -> ErrorPeriodo.DESDE_INVALIDA
            hasta == null -> ErrorPeriodo.HASTA_INVALIDA
            desde > hasta -> ErrorPeriodo.RANGO_INVERTIDO
            else -> null
        }
        if (error != null || desde == null || hasta == null) {
            _uiState.update { it.copy(errorPersonalizado = error) }
            return
        }
        _uiState.update { it.copy(periodo = Periodo(desde, hasta), errorPersonalizado = null) }
        cargarResumen(conservarContenido = false)
    }

    /** Only changes which fields are shown: no request. */
    fun onModoCambiado(modo: ModoImporte) {
        _uiState.update { it.copy(modo = modo) }
    }

    // ---- reloading ---------------------------------------------------------------------------

    fun onRefrescar() {
        cargarResumen(conservarContenido = true)
        cargarPendientes(conservarContenido = true)
    }

    fun onReintentarResumen() {
        cargarResumen(conservarContenido = false)
    }

    fun onReintentarPendientes() {
        cargarPendientes(conservarContenido = false)
    }

    /** The detail registered or reverted a cobro (or the ingreso vanished): both sections are stale. */
    fun onVolverDelDetalle() {
        onRefrescar()
    }

    // ---- loading -----------------------------------------------------------------------------

    /**
     * With [conservarContenido] the figures on screen stay until the answer arrives; otherwise the
     * section shows its spinner (a new period must not display the old period's numbers).
     */
    private fun cargarResumen(conservarContenido: Boolean) {
        val periodo = _uiState.value.periodo
        trabajoResumen?.cancel()
        trabajoResumen = viewModelScope.launch {
            if (!conservarContenido || _uiState.value.resumen !is CargaResumen.Contenido) {
                _uiState.update { it.copy(resumen = CargaResumen.Cargando) }
            }
            dashboard.resumen(periodo.desde, periodo.hasta).fold(
                onSuccess = { resumen -> _uiState.update { it.copy(resumen = CargaResumen.Contenido(resumen)) } },
                onFailure = { fallo -> _uiState.update { it.copy(resumen = CargaResumen.Error(fallo.comoErrorAuth())) } },
            )
        }
    }

    private fun cargarPendientes(conservarContenido: Boolean) {
        trabajoPendientes?.cancel()
        trabajoPendientes = viewModelScope.launch {
            val actual = _uiState.value.pendientes
            val hayContenido = actual is CargaPendientes.Contenido || actual is CargaPendientes.Vacio
            if (!conservarContenido || !hayContenido) {
                _uiState.update { it.copy(pendientes = CargaPendientes.Cargando) }
            }
            dashboard.pendientesCobro().fold(
                onSuccess = { pendientes -> _uiState.update { it.copy(pendientes = conNombres(pendientes)) } },
                onFailure = { fallo -> _uiState.update { it.copy(pendientes = CargaPendientes.Error(fallo.comoErrorAuth())) } },
            )
        }
    }

    /** The response has only `clienteId`: names come from the clientes list (best effort, no name if it fails). */
    private suspend fun conNombres(pendientes: PendientesCobro): CargaPendientes {
        if (pendientes.ingresos.isEmpty()) {
            return CargaPendientes.Vacio
        }
        val nombres = clientes.listar().getOrNull().orEmpty().associate { it.id to it.nombre }
        val filas = pendientes.ingresos.map { FilaPendiente(it, it.clienteId?.let { id -> nombres[id] }) }
        return CargaPendientes.Contenido(pendientes.total, filas)
    }

    private fun parsearFecha(texto: String): LocalDate? =
        try {
            LocalDate.parse(texto.trim())
        } catch (e: IllegalArgumentException) {
            null
        }
}
