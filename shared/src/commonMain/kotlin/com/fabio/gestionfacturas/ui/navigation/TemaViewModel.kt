package com.fabio.gestionfacturas.ui.navigation

import androidx.lifecycle.ViewModel
import com.fabio.gestionfacturas.domain.model.PreferenciaTema
import com.fabio.gestionfacturas.domain.repository.TemaRepository
import kotlinx.coroutines.flow.StateFlow

class TemaViewModel(
    private val repositorio: TemaRepository,
) : ViewModel() {

    val preferencia: StateFlow<PreferenciaTema> = repositorio.preferencia

    /** Sistema -> Claro -> Oscuro -> Sistema. */
    fun onCambiarTema() {
        val siguiente = when (repositorio.preferencia.value) {
            PreferenciaTema.SISTEMA -> PreferenciaTema.CLARO
            PreferenciaTema.CLARO -> PreferenciaTema.OSCURO
            PreferenciaTema.OSCURO -> PreferenciaTema.SISTEMA
        }
        repositorio.cambiar(siguiente)
    }
}
