package com.fabio.gestionfacturas.data.local

import com.fabio.gestionfacturas.domain.model.ArchivoSeleccionado
import com.fabio.gestionfacturas.domain.model.TAMANO_MAXIMO_IMAGEN_BYTES
import com.fabio.gestionfacturas.domain.repository.FileOpener
import com.fabio.gestionfacturas.domain.repository.ResultadoSeleccion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException
import javax.swing.SwingUtilities

actual fun crearFileOpener(): FileOpener = FileOpenerJvm()

/**
 * Native open dialog (AWT `FileDialog`) filtered to images; the bytes are read off the UI thread.
 * It does not validate size or format: that is `validarImagen`'s job.
 */
internal class FileOpenerJvm : FileOpener {

    override suspend fun elegirImagen(): ResultadoSeleccion {
        val archivo = elegirArchivo() ?: return ResultadoSeleccion.Cancelado
        try {
            val bytes = withContext(Dispatchers.IO) {
                // A huge file would only be rejected later; do not load it into memory first.
                if (archivo.length() > LIMITE_LECTURA_BYTES) {
                    null
                } else {
                    archivo.readBytes()
                }
            }
            if (bytes == null) {
                return ResultadoSeleccion.Error("El archivo es demasiado grande. El máximo es ${TAMANO_MAXIMO_IMAGEN_BYTES / (1024 * 1024)} MB.")
            }
            return ResultadoSeleccion.Seleccionado(ArchivoSeleccionado(archivo.name, mimeDe(archivo.name), bytes))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return ResultadoSeleccion.Error(MENSAJE_LECTURA)
        } catch (e: SecurityException) {
            return ResultadoSeleccion.Error(MENSAJE_LECTURA)
        }
    }

    /** The dialog is modal and must run on the AWT event thread; null means the user cancelled. */
    private suspend fun elegirArchivo(): File? =
        withContext(Dispatchers.Default) {
            var elegido: File? = null
            SwingUtilities.invokeAndWait {
                val dialogo = FileDialog(null as Frame?, "Elegir imagen", FileDialog.LOAD)
                // Windows' native dialog ignores the filename filter but honours a wildcard in `file`.
                dialogo.setFilenameFilter { _, nombre -> extensionDe(nombre) in EXTENSIONES }
                dialogo.file = "*.jpg;*.jpeg;*.png"
                carpetaInicial()?.let { dialogo.directory = it.absolutePath }
                dialogo.isVisible = true
                val nombre = dialogo.file
                val carpeta = dialogo.directory
                if (nombre != null && carpeta != null) {
                    elegido = File(carpeta, nombre)
                }
                dialogo.dispose()
            }
            elegido
        }

    private fun carpetaInicial(): File? {
        val casa = System.getProperty("user.home") ?: return null
        val candidatas = listOf("Pictures", "Imágenes", "Downloads", "Descargas", "Documents", "Documentos")
        return candidatas.map { File(casa, it) }.firstOrNull { it.isDirectory } ?: File(casa).takeIf { it.isDirectory }
    }

    private fun extensionDe(nombre: String): String = nombre.substringAfterLast('.', "").lowercase()

    /** From the extension: `probeContentType` depends on the OS. Unknown extensions are rejected later by `validarImagen`. */
    private fun mimeDe(nombre: String): String =
        when (extensionDe(nombre)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }

    private companion object {
        val EXTENSIONES = setOf("jpg", "jpeg", "png")
        const val LIMITE_LECTURA_BYTES = 4L * TAMANO_MAXIMO_IMAGEN_BYTES
        const val MENSAJE_LECTURA = "No se pudo leer el archivo. Comprueba que existe y que tienes permiso para abrirlo."
    }
}
