package com.fabio.gestionfacturas.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.gestionfacturas.domain.model.DatosGasto
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.NuevoGasto
import com.fabio.gestionfacturas.domain.repository.CategoriaRepository
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.domain.repository.GastoRepository
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.common.comoErrorAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** One form for both modes: [idGasto] null creates, otherwise it loads and edits that gasto. */
class GastoFormViewModel(
    private val idGasto: Long?,
    private val gastos: GastoRepository,
    private val clientes: ClienteRepository,
    private val categorias: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GastoFormUiState(
            idGasto = idGasto,
            carga = if (idGasto == null) CargaFormGasto.Lista else CargaFormGasto.Cargando,
        ),
    )
    val uiState: StateFlow<GastoFormUiState> = _uiState.asStateFlow()

    init {
        if (idGasto != null) {
            cargar(idGasto)
        }
        viewModelScope.launch { cargarSelectores() }
    }

    fun onReintentarCarga() {
        if (idGasto == null || _uiState.value.carga !is CargaFormGasto.Error) {
            return
        }
        cargar(idGasto)
    }

    fun onEmisorChange(valor: String) = editarCampo(CampoGasto.EMISOR) { it.copy(emisor = valor) }

    fun onFechaEmisionChange(valor: String) = editarCampo(CampoGasto.FECHA_EMISION) { it.copy(fechaEmision = valor) }

    fun onBaseImponibleChange(valor: String) = editarCampo(CampoGasto.BASE_IMPONIBLE) { it.copy(baseImponible = valor) }

    fun onIvaChange(valor: String) = editarCampo(CampoGasto.IVA) { it.copy(iva = valor) }

    fun onTotalChange(valor: String) = editarCampo(CampoGasto.TOTAL) { it.copy(total = valor) }

    fun onMonedaChange(valor: String) = editarCampo(CampoGasto.MONEDA) { it.copy(moneda = valor) }

    fun onDeducibleChange(valor: Boolean) = editarCampo(null) { it.copy(deducible = valor) }

    fun onClienteChange(id: Long?) = editarCampo(CampoGasto.CLIENTE) { it.copy(clienteId = id) }

    fun onCategoriaChange(id: Long?) = editarCampo(CampoGasto.CATEGORIA) { it.copy(categoriaId = id) }

    fun onRecargarSelectores() {
        if (_uiState.value.recargandoSelectores) {
            return
        }
        viewModelScope.launch { cargarSelectores() }
    }

    fun onGuardar() {
        val estado = _uiState.value
        if (estado.guardando || estado.carga != CargaFormGasto.Lista) {
            return
        }
        val total = Importe.de(estado.total)
        val base = importeOpcional(estado.baseImponible)
        val iva = importeOpcional(estado.iva)
        val fecha = fechaOpcional(estado.fechaEmision)
        val errores = buildMap {
            if (estado.total.isBlank()) {
                put(CampoGasto.TOTAL, ErrorCampoGasto.TOTAL_VACIO)
            } else if (total == null) {
                put(CampoGasto.TOTAL, ErrorCampoGasto.IMPORTE_INVALIDO)
            }
            if (base == InvalidoImporte) {
                put(CampoGasto.BASE_IMPONIBLE, ErrorCampoGasto.IMPORTE_INVALIDO)
            }
            if (iva == InvalidoImporte) {
                put(CampoGasto.IVA, ErrorCampoGasto.IMPORTE_INVALIDO)
            }
            if (fecha == FechaInvalida) {
                put(CampoGasto.FECHA_EMISION, ErrorCampoGasto.FECHA_INVALIDA)
            }
        }
        if (errores.isNotEmpty() || total == null) {
            _uiState.update { it.copy(erroresLocales = errores, error = null) }
            return
        }
        val emisor = estado.emisor.trim().ifEmpty { null }
        val moneda = estado.moneda.trim().ifEmpty { null }
        val baseImponible = (base as? ImporteValido)?.importe
        val ivaImporte = (iva as? ImporteValido)?.importe
        val fechaEmision = (fecha as? FechaValida)?.fecha

        _uiState.update { it.copy(guardando = true, erroresLocales = emptyMap(), error = null) }
        viewModelScope.launch {
            val resultado = if (idGasto == null) {
                gastos.crear(
                    NuevoGasto(
                        total = total,
                        emisor = emisor,
                        fechaEmision = fechaEmision,
                        baseImponible = baseImponible,
                        iva = ivaImporte,
                        moneda = moneda,
                        clienteId = estado.clienteId,
                    ),
                )
            } else {
                gastos.editar(
                    idGasto,
                    DatosGasto(
                        total = total,
                        emisor = emisor,
                        fechaEmision = fechaEmision,
                        baseImponible = baseImponible,
                        iva = ivaImporte,
                        moneda = moneda,
                        clienteId = estado.clienteId,
                        categoriaId = estado.categoriaId,
                        deducible = estado.deducible,
                    ),
                )
            }
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(guardando = false, salida = ResultadoFormulario.GUARDADO) } },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    // A 404 on save can be the gasto, the category or the cliente: the backend's message says
                    // which, and the selectors may be stale, so offer to reload them.
                    val selectoresViejos = error is ErrorAuth.NoEncontradoConDetalle || error == ErrorAuth.NoEncontrado
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            error = error,
                            selectoresFallidos = it.selectoresFallidos || selectoresViejos,
                        )
                    }
                },
            )
        }
    }

    fun onSalidaConsumida() {
        _uiState.update { it.copy(salida = null) }
    }

    private fun cargar(id: Long) {
        _uiState.update { it.copy(carga = CargaFormGasto.Cargando) }
        viewModelScope.launch {
            gastos.obtener(id).fold(
                onSuccess = { gasto -> _uiState.update { it.conDatosDe(gasto) } },
                onFailure = { fallo ->
                    val error = fallo.comoErrorAuth()
                    if (error == ErrorAuth.NoEncontrado || error is ErrorAuth.NoEncontradoConDetalle) {
                        _uiState.update { it.copy(salida = ResultadoFormulario.NO_ENCONTRADO) }
                    } else {
                        _uiState.update { it.copy(carga = CargaFormGasto.Error(error)) }
                    }
                },
            )
        }
    }

    /** Both lists are best effort: the form works without them, and a failure only offers a reload. */
    private suspend fun cargarSelectores() {
        _uiState.update { it.copy(recargandoSelectores = true) }
        val listaClientes = clientes.listar()
        val listaCategorias = categorias.listar()
        _uiState.update {
            it.copy(
                recargandoSelectores = false,
                clientes = listaClientes.getOrNull()?.filter { cliente -> cliente.activo } ?: it.clientes,
                categorias = listaCategorias.getOrNull() ?: it.categorias,
                selectoresFallidos = listaClientes.isFailure || listaCategorias.isFailure,
            )
        }
    }

    /** Editing a field clears its own local error and the general error. */
    private fun editarCampo(campo: CampoGasto?, cambio: (GastoFormUiState) -> GastoFormUiState) {
        _uiState.update {
            val erroresLocales = if (campo == null) it.erroresLocales else it.erroresLocales - campo
            cambio(it).copy(erroresLocales = erroresLocales, error = null)
        }
    }

    /** The total is not returned by the backend, so it stays empty and must be typed again. */
    private fun GastoFormUiState.conDatosDe(gasto: Gasto) = copy(
        carga = CargaFormGasto.Lista,
        emisor = gasto.emisor.orEmpty(),
        fechaEmision = gasto.fechaEmision?.toString().orEmpty(),
        baseImponible = gasto.baseImponible?.valor.orEmpty(),
        iva = gasto.iva?.valor.orEmpty(),
        moneda = gasto.moneda ?: MONEDA_POR_DEFECTO,
        deducible = gasto.deducible ?: false,
        clienteId = gasto.clienteId,
        categoriaId = gasto.categoriaId,
    )

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
