package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.ArchivoSeleccionado
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.ResultadoValidacion

enum class FaseDigitalizar { INICIAL, SELECCIONADO, SUBIENDO, ERROR }

/** What went wrong, as the screen needs to tell it apart (the texts live in `MensajesError.kt`). */
sealed interface ErrorDigitalizar {
    data object ArchivoVacio : ErrorDigitalizar
    data object FormatoNoSoportado : ErrorDigitalizar
    data object EsPdf : ErrorDigitalizar
    data object DemasiadoGrande : ErrorDigitalizar

    /** 422: the server could not read the invoice; the user can create the gasto by hand instead. */
    data object NoSePudoLeer : ErrorDigitalizar

    /** 429: never retried automatically. */
    data class DemasiadasPeticiones(val reintentarEnSegundos: Int?) : ErrorDigitalizar
    data object SinConexion : ErrorDigitalizar
    data object Inesperado : ErrorDigitalizar

    /** The file picker itself failed; [mensaje] is already user-facing Spanish text. */
    data class Seleccion(val mensaje: String) : ErrorDigitalizar
}

data class DigitalizarUiState(
    val fase: FaseDigitalizar = FaseDigitalizar.INICIAL,
    /** Kept after an upload error so the user can retry; null when the picked file was rejected locally. */
    val archivo: ArchivoSeleccionado? = null,
    val error: ErrorDigitalizar? = null,
    /** One-shot effect: the id of the gasto the backend created. The screen navigates and clears it. */
    val gastoCreadoId: Long? = null,
) {
    val subiendo: Boolean get() = fase == FaseDigitalizar.SUBIENDO

    /** The back control is disabled while the upload is in flight. */
    val puedeVolver: Boolean get() = !subiendo
}

internal fun ResultadoValidacion.comoErrorDigitalizar(): ErrorDigitalizar? =
    when (this) {
        ResultadoValidacion.Valida -> null
        ResultadoValidacion.Vacio -> ErrorDigitalizar.ArchivoVacio
        ResultadoValidacion.EsPdf -> ErrorDigitalizar.EsPdf
        ResultadoValidacion.FormatoNoSoportado -> ErrorDigitalizar.FormatoNoSoportado
        is ResultadoValidacion.DemasiadoGrande -> ErrorDigitalizar.DemasiadoGrande
    }

internal fun ErrorAuth.comoErrorDigitalizar(): ErrorDigitalizar =
    when (this) {
        is ErrorAuth.ArchivoDemasiadoGrande -> ErrorDigitalizar.DemasiadoGrande
        is ErrorAuth.NoSePudoLeer -> ErrorDigitalizar.NoSePudoLeer
        is ErrorAuth.DemasiadasPeticiones -> ErrorDigitalizar.DemasiadasPeticiones(reintentarEnSegundos)
        ErrorAuth.ServidorInaccesible -> ErrorDigitalizar.SinConexion
        else -> ErrorDigitalizar.Inesperado
    }

private const val BYTES_POR_KB = 1024L
private const val BYTES_POR_MB = 1024L * 1024L

/** `512 B`, `350 KB`, `2,3 MB`: integer operations only, one decimal for megabytes (truncated). */
fun formatearTamano(bytes: Int): String {
    val total = bytes.toLong()
    if (total < BYTES_POR_KB) {
        return "$total B"
    }
    if (total < BYTES_POR_MB) {
        return "${total / BYTES_POR_KB} KB"
    }
    val decimas = total * 10L / BYTES_POR_MB
    return "${decimas / 10L},${decimas % 10L} MB"
}
