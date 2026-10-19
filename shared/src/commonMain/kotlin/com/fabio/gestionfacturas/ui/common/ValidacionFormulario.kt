package com.fabio.gestionfacturas.ui.common

import com.fabio.gestionfacturas.domain.model.ErrorAuth

/** The inputs of the auth forms; also the keys the backend uses for field errors. */
enum class CampoFormulario(val claveApi: String) {
    EMAIL("email"),
    NOMBRE("nombre"),
    PASSWORD("password"),
}

/** What is wrong with a field before even calling the backend. */
enum class ErrorCampo {
    EMAIL_VACIO,
    EMAIL_INVALIDO,
    NOMBRE_VACIO,
    PASSWORD_VACIA,
}

/** Basic client-side checks only: the backend remains the authority on what is valid. */
object ValidacionFormulario {

    fun email(valor: String): ErrorCampo? {
        val limpio = valor.trim()
        if (limpio.isEmpty()) {
            return ErrorCampo.EMAIL_VACIO
        }
        val arroba = limpio.indexOf('@')
        val puntoDespues = limpio.lastIndexOf('.')
        val tieneFormaDeEmail = arroba > 0 && puntoDespues > arroba + 1 && puntoDespues < limpio.length - 1
        if (!tieneFormaDeEmail || limpio.contains(' ')) {
            return ErrorCampo.EMAIL_INVALIDO
        }
        return null
    }

    fun nombre(valor: String): ErrorCampo? {
        if (valor.isBlank()) {
            return ErrorCampo.NOMBRE_VACIO
        }
        return null
    }

    fun password(valor: String): ErrorCampo? {
        if (valor.isEmpty()) {
            return ErrorCampo.PASSWORD_VACIA
        }
        return null
    }
}

/** Anything that is not already a typed auth error is reported as unexpected. */
fun Throwable.comoErrorAuth(): ErrorAuth {
    if (this is ErrorAuth) {
        return this
    }
    return ErrorAuth.Inesperado
}
