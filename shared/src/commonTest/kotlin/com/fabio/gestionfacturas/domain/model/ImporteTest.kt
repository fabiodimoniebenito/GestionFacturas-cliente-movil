package com.fabio.gestionfacturas.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImporteTest {

    @Test
    fun aceptaPuntoYComaYNormalizaAPunto() {
        assertEquals("121.50", Importe.de("121.50")?.valor)
        assertEquals("121.50", Importe.de("121,50")?.valor)
        assertEquals("5.5", Importe.de(" 5,5 ")?.valor)
        assertEquals("7", Importe.de("7")?.valor)
        assertEquals("0.99", Importe.de("0,99")?.valor)
    }

    @Test
    fun conservaLosCerosFinales() {
        assertEquals("121.00", Importe.de("121,00")?.valor)
    }

    @Test
    fun rechazaMasDeDosDecimales() {
        assertNull(Importe.de("1.234"))
        assertNull(Importe.de("1,234"))
    }

    @Test
    fun rechazaTextoNoNumerico() {
        assertNull(Importe.de(""))
        assertNull(Importe.de("   "))
        assertNull(Importe.de("abc"))
        assertNull(Importe.de("12a"))
        assertNull(Importe.de("1.2.3"))
        assertNull(Importe.de("1,2,3"))
        assertNull(Importe.de("12."))
        assertNull(Importe.de(",5"))
        assertNull(Importe.de("-5"))
        assertNull(Importe.de("1e3"))
    }

    @Test
    fun mostrarUsaComaDecimal() {
        assertEquals("121,00", Importe.de("121.00")?.mostrar())
        assertEquals("7", Importe.de("7")?.mostrar())
    }
}
