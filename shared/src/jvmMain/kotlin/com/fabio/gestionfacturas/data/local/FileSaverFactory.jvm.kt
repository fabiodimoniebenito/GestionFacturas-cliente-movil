package com.fabio.gestionfacturas.data.local

import com.fabio.gestionfacturas.domain.repository.FileSaver
import com.fabio.gestionfacturas.domain.repository.ResultadoGuardado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException
import javax.swing.SwingUtilities

actual fun crearFileSaver(): FileSaver = FileSaverJvm()

/** Native save dialog (AWT `FileDialog`) proposing the name in Downloads or Documents; the write runs off the UI thread. */
internal class FileSaverJvm : FileSaver {

    override suspend fun guardar(nombreSugerido: String, mime: String, bytes: ByteArray): ResultadoGuardado {
        val destino = elegirDestino(nombreSugerido) ?: return ResultadoGuardado.Cancelado
        try {
            withContext(Dispatchers.IO) { destino.writeBytes(bytes) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return ResultadoGuardado.Error("No se pudo guardar el archivo. Comprueba que la carpeta admite escritura.")
        } catch (e: SecurityException) {
            return ResultadoGuardado.Error("No se pudo guardar el archivo. Comprueba que la carpeta admite escritura.")
        }
        return ResultadoGuardado.Guardado(destino.absolutePath)
    }

    /** The dialog is modal and must run on the AWT event thread; null means the user cancelled. */
    private suspend fun elegirDestino(nombreSugerido: String): File? =
        withContext(Dispatchers.Default) {
            var elegido: File? = null
            SwingUtilities.invokeAndWait {
                val dialogo = FileDialog(null as Frame?, "Guardar", FileDialog.SAVE)
                dialogo.file = nombreSugerido
                carpetaInicial()?.let { dialogo.directory = it.absolutePath }
                dialogo.isVisible = true
                val archivo = dialogo.file
                val carpeta = dialogo.directory
                if (archivo != null && carpeta != null) {
                    elegido = File(carpeta, archivo)
                }
                dialogo.dispose()
            }
            elegido
        }

    private fun carpetaInicial(): File? {
        val casa = System.getProperty("user.home") ?: return null
        val candidatas = listOf("Downloads", "Descargas", "Documents", "Documentos")
        return candidatas.map { File(casa, it) }.firstOrNull { it.isDirectory } ?: File(casa).takeIf { it.isDirectory }
    }
}
