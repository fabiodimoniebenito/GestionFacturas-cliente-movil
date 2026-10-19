package com.fabio.gestionfacturas.ui.login

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.ui.common.CampoFormulario
import com.fabio.gestionfacturas.ui.common.ErrorCampo

/**
 * Everything the registration screen renders. [registroExitoso] is the one-shot "navigate to Home"
 * effect (registering also logs the user in); the screen clears it via
 * [RegistroViewModel.onNavegacionConsumida].
 */
data class RegistroUiState(
    val email: String = "",
    val nombre: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val erroresCampo: Map<CampoFormulario, ErrorCampo> = emptyMap(),
    val error: ErrorAuth? = null,
    val registroExitoso: Boolean = false,
)
