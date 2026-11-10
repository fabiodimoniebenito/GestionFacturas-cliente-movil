package com.fabio.gestionfacturas.domain.model

/** The ONE place that defines the largest image accepted for digitalizar (10 MB). */
const val TAMANO_MAXIMO_IMAGEN_BYTES: Int = 10 * 1024 * 1024

sealed interface ResultadoValidacion {
    data object Valida : ResultadoValidacion
    data object Vacio : ResultadoValidacion

    /** PDFs are recognised so the UI can say they are not supported yet. */
    data object EsPdf : ResultadoValidacion
    data object FormatoNoSoportado : ResultadoValidacion
    data class DemasiadoGrande(val limiteBytes: Int) : ResultadoValidacion
}

private val EXTENSIONES_IMAGEN = setOf("jpg", "jpeg", "png")
private val MIMES_IMAGEN = setOf("image/jpeg", "image/png")
private const val MIME_PDF = "application/pdf"

/**
 * Pure client-side check before uploading. Order: empty, PDF, format (extension AND mime must both
 * be jpg/jpeg/png), size (exactly [TAMANO_MAXIMO_IMAGEN_BYTES] is accepted).
 */
fun validarImagen(archivo: ArchivoSeleccionado): ResultadoValidacion {
    if (archivo.bytes.isEmpty()) {
        return ResultadoValidacion.Vacio
    }
    val extension = archivo.nombre.substringAfterLast('.', "").trim().lowercase()
    val mime = archivo.mime.substringBefore(';').trim().lowercase()
    if (extension == "pdf" || mime == MIME_PDF) {
        return ResultadoValidacion.EsPdf
    }
    if (extension !in EXTENSIONES_IMAGEN || mime !in MIMES_IMAGEN) {
        return ResultadoValidacion.FormatoNoSoportado
    }
    if (archivo.bytes.size > TAMANO_MAXIMO_IMAGEN_BYTES) {
        return ResultadoValidacion.DemasiadoGrande(TAMANO_MAXIMO_IMAGEN_BYTES)
    }
    return ResultadoValidacion.Valida
}
