package com.fabio.gestionfacturas.domain.model

/** The logged-in user. Tokens are an implementation detail of the data layer and never leave it. */
data class Sesion(
    val usuarioId: Long,
    val email: String,
)
