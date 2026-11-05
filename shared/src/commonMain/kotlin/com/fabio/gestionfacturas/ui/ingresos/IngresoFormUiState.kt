package com.fabio.gestionfacturas.ui.ingresos

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth

/** The inputs; also the keys the backend uses for field errors. */
enum class CampoIngreso(val claveApi: String) {
    CONCEPTO("concepto"),
    FECHA_EMISION("fechaEmision"),
    BASE_IMPONIBLE("baseImponible"),
    IVA("iva"),
    TOTAL("total"),
    MONEDA("moneda"),
    CLIENTE("clienteId"),
}

/** What is wrong with a field before even calling the backend. */
enum class ErrorCampoIngreso {
    TOTAL_VACIO,
    IMPORTE_INVALIDO,
    FECHA_INVALIDA,
    CLIENTE_VACIO,
}

const val MONEDA_INGRESO_POR_DEFECTO = "EUR"

data class IngresoFormUiState(
    val concepto: String = "",
    /** `yyyy-MM-dd` text, checked on save. */
    val fechaEmision: String = "",
    val baseImponible: String = "",
    val iva: String = "",
    val total: String = "",
    val moneda: String = MONEDA_INGRESO_POR_DEFECTO,
    val clienteId: Long? = null,
    /** Active clientes only. */
    val clientes: List<Cliente> = emptyList(),
    /** The clientes are being loaded (first load or a reload). */
    val cargandoClientes: Boolean = true,
    /** Loading the clientes failed. */
    val clientesFallidos: Boolean = false,
    val erroresLocales: Map<CampoIngreso, ErrorCampoIngreso> = emptyMap(),
    val guardando: Boolean = false,
    val error: ErrorAuth? = null,
    /** The id of the ingreso just created: the screen goes to its detail once, then clears this. */
    val creadoId: Long? = null,
) {
    /** A cliente must exist first: the list loaded fine and has no active cliente. */
    val sinClientesActivos: Boolean
        get() = !cargandoClientes && !clientesFallidos && clientes.isEmpty()
}
