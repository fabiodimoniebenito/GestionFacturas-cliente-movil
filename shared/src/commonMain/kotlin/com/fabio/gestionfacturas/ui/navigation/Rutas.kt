package com.fabio.gestionfacturas.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe destinations. Navigation lives only in this package so the library can be swapped later. */

// Outer graph: login, registro and the logged-in shell.
@Serializable
data object RutaLogin

@Serializable
data object RutaRegistro

@Serializable
data object RutaShell

// Inner graph: the content area of the shell.
@Serializable
data object RutaClientes

@Serializable
data object RutaNuevoCliente

@Serializable
data class RutaEditarCliente(val id: Long)
