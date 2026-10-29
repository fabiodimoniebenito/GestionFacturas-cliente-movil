package com.fabio.gestionfacturas.domain.model

import kotlin.jvm.JvmInline

/**
 * A money amount kept as TEXT, with a dot as decimal separator (e.g. "121.00"). The client never
 * does arithmetic on amounts (the backend computes totals), so there is no numeric type here.
 *
 * User input goes through [de], which validates and normalizes. The constructor is internal for the
 * data layer, which wraps whatever literal the backend sent without altering it.
 */
@JvmInline
value class Importe internal constructor(val valor: String) {

    /** The amount for display: comma as decimal separator ("121,00"). Strings only. */
    fun mostrar(): String = valor.replace('.', ',')

    override fun toString(): String = valor

    companion object {
        private val FORMATO = Regex("""\d+([.,]\d{1,2})?""")

        /**
         * Parses what the user typed: digits with an optional decimal part of at most 2 digits,
         * separated by a comma or a dot. Returns null if it is not valid. The result uses a dot.
         */
        fun de(texto: String): Importe? {
            val limpio = texto.trim()
            if (!FORMATO.matches(limpio)) {
                return null
            }
            return Importe(limpio.replace(',', '.'))
        }
    }
}
