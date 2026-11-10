package com.fabio.gestionfacturas.data.local

import com.fabio.gestionfacturas.domain.repository.FileOpener
import com.fabio.gestionfacturas.domain.repository.ResultadoSeleccion

actual fun crearFileOpener(): FileOpener = FileOpenerAndroid()

/**
 * PENDING (Android phase): picking needs the Activity Result API (`GetContent`), and
 * `registerForActivityResult` must be called while the Activity is being created, so wiring it to a
 * Koin singleton means changing `MainActivity`. Until then it reports a clear error.
 */
private class FileOpenerAndroid : FileOpener {
    override suspend fun elegirImagen(): ResultadoSeleccion =
        ResultadoSeleccion.Error("Elegir archivos aún no está disponible en Android.")
}
