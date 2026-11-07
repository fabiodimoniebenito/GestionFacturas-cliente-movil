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
    fun cambiarTemaRecorreSistemaClaroOscuro() {
        val viewModel = TemaViewModel(TemaFalso(PreferenciaTema.SISTEMA))
        val vistos = buildList {
            repeat(3) {
                viewModel.onCambiarTema()
                add(viewModel.preferencia.value)
            }
        }
        assertEquals(listOf(PreferenciaTema.CLARO, PreferenciaTema.OSCURO, PreferenciaTema.SISTEMA), vistos)
    }
}
