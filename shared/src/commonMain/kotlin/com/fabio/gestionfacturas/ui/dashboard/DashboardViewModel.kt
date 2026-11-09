package com.fabio.gestionfacturas.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.PendientesCobro
import com.fabio.gestionfacturas.domain.model.RentabilidadCliente
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.domain.repository.DashboardRepository
import com.fabio.gestionfacturas.domain.repository.ExportRepository
import com.fabio.gestionfacturas.domain.repository.FileSaver
import com.fabio.gestionfacturas.domain.repository.ResultadoGuardado
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
    private val exportacion: ExportRepository,
    private val archivos: FileSaver,
    private val hoy: () -> LocalDate = ::fechaDeHoy,
) : ViewModel() {

    private val _uiState = MutableStateFlow(estadoInicial())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var trabajoResumen: Job? = null
    private var trabajoPendientes: Job? = null
    private var trabajoRentabilidad: Job? = null
    private var trabajoExportacion: Job? = null

    init {
        cargarPeriodo(conservarContenido = false)
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
        _uiState.update { it.copy(preset = preset, periodo = periodo, errorPersonalizado = null, mensajeExportacion = null) }
        cargarPeriodo(conservarContenido = false)
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
        _uiState.update { it.copy(periodo = Periodo(desde, hasta), errorPersonalizado = null, mensajeExportacion = null) }
        cargarPeriodo(conservarContenido = false)
    }

    /** Only changes which fields are shown: no request. */
    fun onModoCambiado(modo: ModoImporte) {
        _uiState.update { it.copy(modo = modo) }
    }

    // ---- reloading ---------------------------------------------------------------------------

    fun onRefrescar() {
        cargarPeriodo(conservarContenido = true)
        cargarPendientes(conservarContenido = true)
    }

    fun onReintentarResumen() {
        cargarResumen(conservarContenido = false)
    }

    fun onReintentarRentabilidad() {
        cargarRentabilidad(conservarContenido = false)
    }

    fun onReintentarPendientes() {
        cargarPendientes(conservarContenido = false)
    }

    /** The detail registered or reverted a cobro (or the ingreso vanished): both sections are stale. */
    fun onVolverDelDetalle() {
        onRefrescar()
    }

    // ---- export ------------------------------------------------------------------------------

    /**
     * Exports the APPLIED period (never the unapplied custom fields). Ignored while another export
     * runs or while the custom range is invalid. Cancelling the save dialog shows nothing.
     */
    fun onExportar() {
        val estado = _uiState.value
        if (!estado.puedeExportar) {
            return
        }
        val periodo = estado.periodo
        _uiState.update { it.copy(exportando = true, mensajeExportacion = null) }
        trabajoExportacion = viewModelScope.launch {
            val mensaje = exportacion.exportarExcel(periodo.desde, periodo.hasta).fold(
                onSuccess = { archivo ->
                    when (val resultado = archivos.guardar(archivo.nombre, archivo.mime, archivo.bytes)) {
                        is ResultadoGuardado.Guardado -> MensajeExportacion.Guardado(resultado.destino)
                        ResultadoGuardado.Cancelado -> null
                        is ResultadoGuardado.Error -> MensajeExportacion.FalloAlGuardar(resultado.mensaje)
                    }
                },
                onFailure = { fallo -> MensajeExportacion.Fallo(fallo.comoErrorAuth()) },
            )
            _uiState.update { it.copy(exportando = false, mensajeExportacion = mensaje) }
        }
    }

    // ---- loading -----------------------------------------------------------------------------

    /** The two sections that depend on the period; each loads and fails on its own. */
    private fun cargarPeriodo(conservarContenido: Boolean) {
        cargarResumen(conservarContenido)
        cargarRentabilidad(conservarContenido)
    }

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

    private fun cargarRentabilidad(conservarContenido: Boolean) {
        val periodo = _uiState.value.periodo
        trabajoRentabilidad?.cancel()
        trabajoRentabilidad = viewModelScope.launch {
            val actual = _uiState.value.rentabilidad
            val hayContenido = actual is CargaRentabilidad.Contenido || actual is CargaRentabilidad.Vacio
            if (!conservarContenido || !hayContenido) {
                _uiState.update { it.copy(rentabilidad = CargaRentabilidad.Cargando) }
            }
            dashboard.rentabilidad(periodo.desde, periodo.hasta).fold(
                onSuccess = { entradas -> _uiState.update { it.copy(rentabilidad = conNombres(entradas)) } },
                onFailure = { fallo -> _uiState.update { it.copy(rentabilidad = CargaRentabilidad.Error(fallo.comoErrorAuth())) } },
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

    /** Keeps the backend order. The API has no name, so it is filled in from the clientes list (best effort). */
    private suspend fun conNombres(entradas: List<RentabilidadCliente>): CargaRentabilidad {
        if (entradas.isEmpty()) {
            return CargaRentabilidad.Vacio
        }
        val nombres = clientes.listar().getOrNull().orEmpty().associate { it.id to it.nombre }
        return CargaRentabilidad.Contenido(
            entradas.map { entrada -> entrada.copy(clienteNombre = entrada.clienteId?.let { id -> nombres[id] }) },
        )
    }

    private fun parsearFecha(texto: String): LocalDate? =
        try {
            LocalDate.parse(texto.trim())
        } catch (e: IllegalArgumentException) {
            null
        }
}
