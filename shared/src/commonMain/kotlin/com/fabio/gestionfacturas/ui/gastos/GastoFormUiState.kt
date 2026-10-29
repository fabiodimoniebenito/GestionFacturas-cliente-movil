package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.Categoria
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario

/** The text inputs; also the keys the backend uses for field errors. */
enum class CampoGasto(val claveApi: String) {
    EMISOR("emisor"),
    FECHA_EMISION("fechaEmision"),
    BASE_IMPONIBLE("baseImponible"),
    IVA("iva"),
    TOTAL("total"),
    MONEDA("moneda"),
    CLIENTE("clienteId"),
    CATEGORIA("categoriaId"),
}

/** What is wrong with a field before even calling the backend. */
enum class ErrorCampoGasto {
    TOTAL_VACIO,
    IMPORTE_INVALIDO,
    FECHA_INVALIDA,
}

/** Edit mode loads the gasto first; create mode starts [Lista]. */
sealed interface CargaFormGasto {
    data object Cargando : CargaFormGasto
    data object Lista : CargaFormGasto
    data class Error(val error: ErrorAuth) : CargaFormGasto
}

const val MONEDA_POR_DEFECTO = "EUR"

data class GastoFormUiState(
    /** Null when creating; the id of the gasto being edited otherwise. */
    val idGasto: Long? = null,
    val carga: CargaFormGasto = CargaFormGasto.Lista,
    val emisor: String = "",
    /** `yyyy-MM-dd` text, checked on save. */
    val fechaEmision: String = "",
    val baseImponible: String = "",
    val iva: String = "",
    /** Never prefilled on edit: the backend does not return it. */
    val total: String = "",
    val moneda: String = MONEDA_POR_DEFECTO,
    val deducible: Boolean = false,
    val clienteId: Long? = null,
    val categoriaId: Long? = null,
    /** Active clientes only. */
    val clientes: List<Cliente> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    /** Loading the clientes or categories failed, or a save said one of them no longer exists. */
    val selectoresFallidos: Boolean = false,
    val recargandoSelectores: Boolean = false,
    val erroresLocales: Map<CampoGasto, ErrorCampoGasto> = emptyMap(),
    val guardando: Boolean = false,
    val error: ErrorAuth? = null,
    val salida: ResultadoFormulario? = null,
) {
    val esEdicion: Boolean get() = idGasto != null
}
