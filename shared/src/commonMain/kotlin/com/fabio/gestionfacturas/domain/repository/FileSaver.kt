package com.fabio.gestionfacturas.domain.repository

/** Outcome of asking the user where to save a file. */
sealed interface ResultadoGuardado {
    /** [destino] is a human-readable place (e.g. the full path). */
    data class Guardado(val destino: String) : ResultadoGuardado
    data object Cancelado : ResultadoGuardado
    /** [mensaje] is already user-facing Spanish text. */
    data class Error(val mensaje: String) : ResultadoGuardado
}

/** Platform file saving (native dialog on Desktop). Its file-open counterpart will come with digitalizar. */
interface FileSaver {
    suspend fun guardar(nombreSugerido: String, mime: String, bytes: ByteArray): ResultadoGuardado
}
