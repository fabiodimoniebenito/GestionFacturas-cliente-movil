package com.fabio.gestionfacturas.domain.model

/**
 * Everything that can go wrong in an authentication operation, as the UI needs to tell it apart.
 * The data layer maps HTTP statuses and transport failures to these; the message is never shown
 * to the user (the UI picks its own text per type), so it must not carry secrets.
 */
sealed class ErrorAuth(message: String) : Exception(message) {

    /** 401: wrong email or password. */
    data object Credenciales : ErrorAuth("Credenciales inválidas")

    /** 429: [reintentarEnSegundos] comes from `Retry-After` when the server sent a numeric value. */
    data class DemasiadasPeticiones(val reintentarEnSegundos: Int?) : ErrorAuth("Demasiadas peticiones")

    /** 400: field name -> message. A plain `{"error": "..."}` body is reported under the key `error`. */
    data class Validacion(val campos: Map<String, String>) : ErrorAuth("Datos no válidos")

    /** 409: the email is already registered. */
    data object EmailYaRegistrado : ErrorAuth("Email ya registrado")

    /** 404: the resource does not exist (the backend also answers 404 for another user's resource). */
    data object NoEncontrado : ErrorAuth("No encontrado")

    /** 409 on a protected resource, e.g. a cliente whose NIF already exists. */
    data object Conflicto : ErrorAuth("Conflicto")

    /**
     * 404 with the backend's `error` message (null if the body had none). On a save it can mean the
     * gasto, the category or the cliente, so the message is what tells them apart.
     */
    data class NoEncontradoConDetalle(val detalle: String?) : ErrorAuth("No encontrado")

    /** 409 with the backend's `error` message, e.g. an invalid state transition. */
    data class ConflictoConDetalle(val detalle: String?) : ErrorAuth("Conflicto")

    /** The server could not be reached (no network, refused connection, timeout). */
    data object ServidorInaccesible : ErrorAuth("Servidor inaccesible")

    /** Any other status or failure. */
    data object Inesperado : ErrorAuth("Error inesperado")
}
