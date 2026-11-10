package com.fabio.gestionfacturas.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ValidarImagenTest {

    private fun archivo(nombre: String, mime: String, tamano: Int = 100) =
        ArchivoSeleccionado(nombre, mime, ByteArray(tamano) { 1 })

    @Test
    fun unJpgValidoEsValido() {
        assertEquals(ResultadoValidacion.Valida, validarImagen(archivo("factura.jpg", "image/jpeg")))
    }

    @Test
    fun unJpegValidoEsValido() {
        assertEquals(ResultadoValidacion.Valida, validarImagen(archivo("factura.jpeg", "image/jpeg")))
    }

    @Test
    fun unPngValidoEsValido() {
        assertEquals(ResultadoValidacion.Valida, validarImagen(archivo("factura.png", "image/png")))
    }

    @Test
    fun extensionYMimeEnMayusculasSeAceptan() {
        assertEquals(ResultadoValidacion.Valida, validarImagen(archivo("FACTURA.JPG", "IMAGE/JPEG")))
    }

    @Test
    fun unArchivoVacioSeRechaza() {
        assertEquals(ResultadoValidacion.Vacio, validarImagen(archivo("factura.jpg", "image/jpeg", tamano = 0)))
    }

    @Test
    fun unPdfTieneSuPropioResultado() {
        assertEquals(ResultadoValidacion.EsPdf, validarImagen(archivo("factura.pdf", "application/pdf")))
    }

    @Test
    fun unPdfSeReconocePorExtensionAunqueElMimeSeaGenerico() {
        assertEquals(ResultadoValidacion.EsPdf, validarImagen(archivo("factura.pdf", "application/octet-stream")))
    }

    @Test
    fun unPdfSeReconocePorMimeAunqueLaExtensionSeaOtra() {
        assertEquals(ResultadoValidacion.EsPdf, validarImagen(archivo("factura", "application/pdf")))
    }

    @Test
    fun unaExtensionNoSoportadaSeRechaza() {
        assertEquals(ResultadoValidacion.FormatoNoSoportado, validarImagen(archivo("factura.gif", "image/gif")))
        assertEquals(ResultadoValidacion.FormatoNoSoportado, validarImagen(archivo("factura.txt", "image/png")))
        assertEquals(ResultadoValidacion.FormatoNoSoportado, validarImagen(archivo("factura", "image/png")))
    }

    @Test
    fun laExtensionCorrectaConMimeIncorrectoSeRechaza() {
        assertEquals(ResultadoValidacion.FormatoNoSoportado, validarImagen(archivo("factura.png", "text/plain")))
        assertEquals(ResultadoValidacion.FormatoNoSoportado, validarImagen(archivo("factura.jpg", "image/gif")))
    }

    @Test
    fun unByteMasQueElLimiteSeRechaza() {
        val resultado = validarImagen(archivo("grande.png", "image/png", TAMANO_MAXIMO_IMAGEN_BYTES + 1))

        assertEquals(ResultadoValidacion.DemasiadoGrande(TAMANO_MAXIMO_IMAGEN_BYTES), resultado)
    }

    @Test
    fun exactamenteElLimiteSeAcepta() {
        val resultado = validarImagen(archivo("justo.png", "image/png", TAMANO_MAXIMO_IMAGEN_BYTES))

        assertEquals(ResultadoValidacion.Valida, resultado)
    }

    @Test
    fun elLimiteSonDiezMegabytes() {
        assertEquals(10 * 1024 * 1024, TAMANO_MAXIMO_IMAGEN_BYTES)
    }
}
