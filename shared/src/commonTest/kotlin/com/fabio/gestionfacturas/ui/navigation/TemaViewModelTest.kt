package com.fabio.gestionfacturas.ui.navigation

import com.fabio.gestionfacturas.domain.model.PreferenciaTema
import com.fabio.gestionfacturas.domain.repository.TemaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class TemaViewModelTest {

    private class TemaFalso(inicial: PreferenciaTema) : TemaRepository {
        private val _preferencia = MutableStateFlow(inicial)
        override val preferencia: StateFlow<PreferenciaTema> = _preferencia.asStateFlow()
        override fun cambiar(preferencia: PreferenciaTema) {
            _preferencia.value = preferencia
        }
    }

    @Test
    fun elegirTemaGuardaOscuroOClaro() {
        val viewModel = TemaViewModel(TemaFalso(PreferenciaTema.SISTEMA))
        viewModel.onElegirTema(oscuro = true)
        assertEquals(PreferenciaTema.OSCURO, viewModel.preferencia.value)
        viewModel.onElegirTema(oscuro = false)
        assertEquals(PreferenciaTema.CLARO, viewModel.preferencia.value)
    }

    @Test
    fun sistemaSigueElTemaDelSistemaOperativo() {
        assertEquals(true, PreferenciaTema.SISTEMA.esOscuro(sistemaOscuro = true))
        assertEquals(false, PreferenciaTema.SISTEMA.esOscuro(sistemaOscuro = false))
        assertEquals(false, PreferenciaTema.CLARO.esOscuro(sistemaOscuro = true))
        assertEquals(true, PreferenciaTema.OSCURO.esOscuro(sistemaOscuro = false))
    }
}
