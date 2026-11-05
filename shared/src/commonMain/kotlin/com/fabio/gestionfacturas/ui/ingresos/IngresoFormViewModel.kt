package com.fabio.gestionfacturas.ui.ingresos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.NuevoIngreso
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.domain.repository.IngresoRepository
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Create form only: the backend has no edit endpoint for ingresos. */
class IngresoFormViewModel(
    private val ingresos: IngresoRepository,
    private val clientes: ClienteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngresoFormUiState())
    val uiState: StateFlow<IngresoFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { cargarClientes() }
    }

    fun onConceptoChange(valor: String) = editarCampo(CampoIngreso.CONCEPTO) { it.copy(concepto = valor) }

    fun onFechaEmisionChange(valor: String) = editarCampo(CampoIngreso.FECHA_EMISION) { it.copy(fechaEmision = valor) }

    fun onBaseImponibleChange(valor: String) = editarCampo(CampoIngreso.BASE_IMPONIBLE) { it.copy(baseImponible = valor) }

    fun onIvaChange(valor: String) = editarCampo(CampoIngreso.IVA) { it.copy(iva = valor) }

    fun onTotalChange(valor: String) = editarCampo(CampoIngreso.TOTAL) { it.copy(total = valor) }

    fun onMonedaChange(valor: String) = editarCampo(CampoIngreso.MONEDA) { it.copy(moneda = valor) }

    fun onClienteChange(id: Long?) = editarCampo(CampoIngreso.CLIENTE) { it.copy(clienteId = id) }

    /** Also called when the user comes back from creating a cliente. */
    fun onRecargarClientes() {
        if (_uiState.value.cargandoClientes) {
            return
        }
        viewModelScope.launch { cargarClientes() }
    }

    fun onGuardar() {
        val estado = _uiState.value
        if (estado.guardando) {
            return
        }
        val total = Importe.de(estado.total)
        val base = importeOpcional(estado.baseImponible)
        val iva = importeOpcional(estado.iva)
        val fecha = fechaOpcional(estado.fechaEmision)
        val errores = buildMap {
            if (estado.total.isBlank()) {
                put(CampoIngreso.TOTAL, ErrorCampoIngreso.TOTAL_VACIO)
            } else if (total == null) {
                put(CampoIngreso.TOTAL, ErrorCampoIngreso.IMPORTE_INVALIDO)
            }
            if (base == InvalidoImporte) {
                put(CampoIngreso.BASE_IMPONIBLE, ErrorCampoIngreso.IMPORTE_INVALIDO)
            }
            if (iva == InvalidoImporte) {
                put(CampoIngreso.IVA, ErrorCampoIngreso.IMPORTE_INVALIDO)
            }
            if (fecha == FechaInvalida) {
                put(CampoIngreso.FECHA_EMISION, ErrorCampoIngreso.FECHA_INVALIDA)
            }
            if (estado.clienteId == null) {
                put(CampoIngreso.CLIENTE, ErrorCampoIngreso.CLIENTE_VACIO)
            }
        }
        if (errores.isNotEmpty() || total == null) {
            _uiState.update { it.copy(erroresLocales = errores, error = null) }
            return
        }
        val nuevo = NuevoIngreso(
            total = total,
            clienteId = estado.clienteId,
            concepto = estado.concepto.trim().ifEmpty { null },
            fechaEmision = (fecha as? FechaValida)?.fecha,
            baseImponible = (base as? ImporteValido)?.importe,
            iva = (iva as? ImporteValido)?.importe,
            moneda = estado.moneda.trim().ifEmpty { null },
        )

        _uiState.update { it.copy(guardando = true, erroresLocales = emptyMap(), error = null) }
        viewModelScope.launch {
            ingresos.crear(nuevo).fold(
                onSuccess = { creado ->
                    _uiState.update { it.copy(guardando = false, creadoId = creado.id) }
                },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    _uiState.update { it.copy(guardando = false, error = error) }
                    // A 404 means the cliente is gone (deactivated or deleted meanwhile): the selector is stale.
                    if (error == ErrorAuth.NoEncontrado || error is ErrorAuth.NoEncontradoConDetalle) {
                        cargarClientes()
                    }
                },
            )
        }
    }

    fun onCreadoConsumido() {
        _uiState.update { it.copy(creadoId = null) }
    }

    /** Only active clientes are offered; a selected cliente that is no longer in the list is dropped. */
    private suspend fun cargarClientes() {
        _uiState.update { it.copy(cargandoClientes = true) }
        val lista = clientes.listar()
        _uiState.update { estado ->
            val activos = lista.getOrNull()?.filter { it.activo } ?: estado.clientes
            val seleccionado = estado.clienteId?.takeIf { id -> activos.any { it.id == id } }
            estado.copy(
                cargandoClientes = false,
                clientesFallidos = lista.isFailure,
                clientes = activos,
                clienteId = seleccionado,
            )
        }
    }

    /** Editing a field clears its own local error and the general error. */
    private fun editarCampo(campo: CampoIngreso, cambio: (IngresoFormUiState) -> IngresoFormUiState) {
        _uiState.update { cambio(it).copy(erroresLocales = it.erroresLocales - campo, error = null) }
    }

    private sealed interface ImporteOpcional
    private data object SinImporte : ImporteOpcional
    private data object InvalidoImporte : ImporteOpcional
    private data class ImporteValido(val importe: Importe) : ImporteOpcional

    private fun importeOpcional(texto: String): ImporteOpcional {
        if (texto.isBlank()) {
            return SinImporte
        }
        val importe = Importe.de(texto) ?: return InvalidoImporte
        return ImporteValido(importe)
    }

    private sealed interface FechaOpcional
    private data object SinFecha : FechaOpcional
    private data object FechaInvalida : FechaOpcional
    private data class FechaValida(val fecha: LocalDate) : FechaOpcional

    private fun fechaOpcional(texto: String): FechaOpcional {
        if (texto.isBlank()) {
            return SinFecha
        }
        return try {
            FechaValida(LocalDate.parse(texto.trim()))
        } catch (e: IllegalArgumentException) {
            FechaInvalida
        }
    }
}
