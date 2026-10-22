package com.fabio.gestionfacturas.domain.model

/** A client of the user. [activo] is false once deactivated; the backend has no way to reactivate it. */
data class Cliente(
    val id: Long,
    val nombre: String,
    val nif: String,
    val email: String?,
    val telefono: String?,
    val activo: Boolean,
)

/** Data to create a [Cliente]. The NIF is only set here: it cannot be edited afterwards. */
data class NuevoCliente(
    val nombre: String,
    val nif: String,
    val email: String? = null,
    val telefono: String? = null,
)

/** The editable data of a [Cliente]. Deliberately has no NIF. */
data class DatosCliente(
    val nombre: String,
    val email: String? = null,
    val telefono: String? = null,
)
