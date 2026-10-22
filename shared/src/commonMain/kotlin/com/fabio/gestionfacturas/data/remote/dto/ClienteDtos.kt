package com.fabio.gestionfacturas.data.remote.dto

import kotlinx.serialization.Serializable

// Shapes copied from docs/api-contract.md (section 1, request/response schemas). Do not rename fields.
// Optional request fields default to null and are therefore omitted from the JSON (encodeDefaults is off).

@Serializable
data class CrearClienteRequest(
    val nombre: String,
    val nif: String,
    val email: String? = null,
    val telefono: String? = null,
)

/** The NIF cannot be edited, so it is not part of this request. */
@Serializable
data class EditarClienteRequest(
    val nombre: String,
    val email: String? = null,
    val telefono: String? = null,
)

/**
 * The contract declares no `required` list on responses. The fields every cliente must have
 * (id, nombre, nif, activo) are required here: a response without them fails decoding.
 */
@Serializable
data class ClienteResponse(
    val id: Long,
    val usuarioId: Long? = null,
    val nombre: String,
    val nif: String,
    val email: String? = null,
    val telefono: String? = null,
    val activo: Boolean,
)
