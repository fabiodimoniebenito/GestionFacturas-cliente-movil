package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.ArchivoSeleccionado

/** Outcome of asking the user to pick an image. */
sealed interface ResultadoSeleccion {
    data class Seleccionado(val archivo: ArchivoSeleccionado) : ResultadoSeleccion
    data object Cancelado : ResultadoSeleccion

    /** [mensaje] is already user-facing Spanish text. */
    data class Error(val mensaje: String) : ResultadoSeleccion
}

/** Platform file picking (native open dialog on Desktop). Counterpart of [FileSaver]. Does NOT validate the file. */
interface FileOpener {
    suspend fun elegirImagen(): ResultadoSeleccion
}
