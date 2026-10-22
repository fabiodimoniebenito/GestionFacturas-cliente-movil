package com.fabio.gestionfacturas.ui.clientes

import com.fabio.gestionfacturas.domain.model.ErrorAuth

/** The form inputs; also the keys the backend uses for field errors. */
enum class CampoCliente(val claveApi: String) {
    NOMBRE("nombre"),
    NIF("nif"),
    EMAIL("email"),
    TELEFONO("telefono"),
}

/** What is wrong with a field before even calling the backend (required fields only). */
enum class ErrorCampoCliente {
    NOMBRE_VACIO,
    NIF_VACIO,
}

/** How the form ended; the screen consumes it once to leave. */
enum class ResultadoFormulario {
    GUARDADO,
    NO_ENCONTRADO,
}

/** Edit mode loads the cliente first; create mode starts [Lista]. */
sealed interface CargaFormCliente {
    data object Cargando : CargaFormCliente
    data object Lista : CargaFormCliente
    data class Error(val error: ErrorAuth) : CargaFormCliente
}

data class ClienteFormUiState(
    /** Null when creating; the id of the cliente being edited otherwise. */
    val idCliente: Long? = null,
    val carga: CargaFormCliente = CargaFormCliente.Lista,
    val nombre: String = "",
    val nif: String = "",
    val email: String = "",
    val telefono: String = "",
    val erroresLocales: Map<CampoCliente, ErrorCampoCliente> = emptyMap(),
    val guardando: Boolean = false,
    val error: ErrorAuth? = null,
    val salida: ResultadoFormulario? = null,
) {
    val esEdicion: Boolean get() = idCliente != null
}
