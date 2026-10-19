package com.fabio.gestionfacturas.data.remote.dto

import kotlinx.serialization.Serializable

// Shapes copied from docs/api-contract.md (section 1, request/response schemas). Do not rename fields.

@Serializable
data class RegistrarRequest(
    val email: String,
    val nombre: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

/** A login without tokens is a failure, so these are required (a missing one fails decoding). */
@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val usuarioId: Long,
    val email: String,
)

/** Refresh returns ONLY a new access token: the refresh token is not rotated. */
@Serializable
data class RefreshResponse(
    val accessToken: String,
)

/** Register returns the user and no tokens. The contract declares no `required` list, so all optional. */
@Serializable
data class UsuarioResponse(
    val id: Long? = null,
    val email: String? = null,
    val nombre: String? = null,
)
