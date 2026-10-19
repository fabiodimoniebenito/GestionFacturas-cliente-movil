package com.fabio.gestionfacturas.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidacionFormularioTest {

    @Test
    fun emailVacioOSoloEspaciosEsVacio() {
        assertEquals(ErrorCampo.EMAIL_VACIO, ValidacionFormulario.email(""))
        assertEquals(ErrorCampo.EMAIL_VACIO, ValidacionFormulario.email("   "))
    }

    @Test
    fun emailsMalFormadosSonInvalidos() {
        val invalidos = listOf("ana", "ana@", "@example.com", "ana@example", "ana@.com", "ana@example.", "a na@example.com")

        for (valor in invalidos) {
            assertEquals(ErrorCampo.EMAIL_INVALIDO, ValidacionFormulario.email(valor), "debería ser inválido: '$valor'")
        }
    }

    @Test
    fun emailsCorrectosSePasanTambienConEspaciosAlrededor() {
        assertNull(ValidacionFormulario.email("ana@example.com"))
        assertNull(ValidacionFormulario.email("  ana.perez+tag@sub.example.es "))
    }

    @Test
    fun nombreEnBlancoEsVacio() {
        assertEquals(ErrorCampo.NOMBRE_VACIO, ValidacionFormulario.nombre(""))
        assertEquals(ErrorCampo.NOMBRE_VACIO, ValidacionFormulario.nombre("  "))
        assertNull(ValidacionFormulario.nombre("Ana"))
    }

    @Test
    fun laContrasenaSoloSeRechazaSiEstaVacia() {
        assertEquals(ErrorCampo.PASSWORD_VACIA, ValidacionFormulario.password(""))
        assertNull(ValidacionFormulario.password(" "))
        assertNull(ValidacionFormulario.password("secreta"))
    }
}
