package com.fabio.gestionfacturas.ui.navigation

/** The logged-in shell: who is logged in and whether a logout is in progress. */
data class ShellUiState(
    val email: String = "",
    val cerrandoSesion: Boolean = false,
)
