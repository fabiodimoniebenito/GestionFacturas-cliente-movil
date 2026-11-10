package com.fabio.gestionfacturas.domain.model

/** A file the user picked: [mime] is what the platform reported (or derived from the extension). */
class ArchivoSeleccionado(val nombre: String, val mime: String, val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ArchivoSeleccionado) {
            return false
        }
        return nombre == other.nombre && mime == other.mime && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var resultado = nombre.hashCode()
        resultado = 31 * resultado + mime.hashCode()
        resultado = 31 * resultado + bytes.contentHashCode()
        return resultado
    }

    // Never print the content: it can be a scanned invoice.
    override fun toString(): String = "ArchivoSeleccionado(nombre=$nombre, mime=$mime, bytes=${bytes.size})"
}
