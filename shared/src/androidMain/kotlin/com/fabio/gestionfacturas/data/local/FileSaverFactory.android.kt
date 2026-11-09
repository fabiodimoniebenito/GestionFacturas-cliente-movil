package com.fabio.gestionfacturas.data.local

import com.fabio.gestionfacturas.domain.repository.FileSaver
import com.fabio.gestionfacturas.domain.repository.ResultadoGuardado

actual fun crearFileSaver(): FileSaver = FileSaverAndroid()

/**
 * PENDING (Android phase): saving needs the Storage Access Framework (`CreateDocument`), which has to
 * be launched from an Activity result launcher. Until then it reports a clear error.
 */
private class FileSaverAndroid : FileSaver {
    override suspend fun guardar(nombreSugerido: String, mime: String, bytes: ByteArray): ResultadoGuardado =
        ResultadoGuardado.Error("Guardar archivos aún no está disponible en Android.")
}
