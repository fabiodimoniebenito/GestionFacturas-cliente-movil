package com.fabio.gestionfacturas.ui.gastos

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatearTamanoTest {

    @Test
    fun menosDeUnKilobyteSonBytes() {
        assertEquals("0 B", formatearTamano(0))
        assertEquals("512 B", formatearTamano(512))
        assertEquals("1023 B", formatearTamano(1023))
    }

    @Test
    fun kilobytesSinDecimales() {
        assertEquals("1 KB", formatearTamano(1024))
        assertEquals("350 KB", formatearTamano(350 * 1024 + 10))
        assertEquals("1023 KB", formatearTamano(1024 * 1024 - 1))
    }

    @Test
    fun megabytesConUnDecimalYComa() {
        assertEquals("1,0 MB", formatearTamano(1024 * 1024))
        assertEquals("2,5 MB", formatearTamano(2 * 1024 * 1024 + 512 * 1024))
        assertEquals("10,0 MB", formatearTamano(10 * 1024 * 1024))
    }
}
