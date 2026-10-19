package com.fabio.gestionfacturas.ui.login

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.ui.common.CampoFormulario
import com.fabio.gestionfacturas.ui.common.ErrorCampo

/**
 * Everything the login screen renders. [loginExitoso] is the one-shot "navigate to Home" effect:
 * the screen reacts to it and tells the ViewModel to clear it (see [LoginViewModel.onNavegacionConsumida]).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val erroresCampo: Map<CampoFormulario, ErrorCampo> = emptyMap(),
    val error: ErrorAuth? = null,
    val loginExitoso: Boolean = false,
)
