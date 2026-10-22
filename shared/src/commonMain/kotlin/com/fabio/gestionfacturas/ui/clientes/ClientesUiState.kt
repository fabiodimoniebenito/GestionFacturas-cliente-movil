package com.fabio.gestionfacturas.ui.clientes

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth

/** What the list area shows. */
sealed interface CargaClientes {
    data object Cargando : CargaClientes
    data object Vacio : CargaClientes
    data class Contenido(val clientes: List<Cliente>) : CargaClientes
    data class Error(val error: ErrorAuth) : CargaClientes
}

/** One-shot message for the screen to show once and then consume. */
sealed interface MensajeClientes {
    /** The cliente was already gone (404) when deactivating; the list is reloaded. */
    data object YaNoExiste : MensajeClientes
    data class DesactivarFallido(val error: ErrorAuth) : MensajeClientes
    /** A reload failed while a list was already on screen, so the old list is kept. */
    data class ActualizarFallido(val error: ErrorAuth) : MensajeClientes
}

data class ClientesUiState(
    val carga: CargaClientes = CargaClientes.Cargando,
    /** A reload is running while the previous list stays visible. */
    val refrescando: Boolean = false,
    /** Cliente waiting for the user to confirm its deactivation. */
    val clienteADesactivar: Cliente? = null,
    val desactivando: Boolean = false,
    val mensaje: MensajeClientes? = null,
)
