package com.fabio.gestionfacturas.ui.navigation

import androidx.lifecycle.ViewModel
import com.fabio.gestionfacturas.domain.model.PreferenciaTema
import com.fabio.gestionfacturas.domain.repository.TemaRepository
import kotlinx.coroutines.flow.StateFlow

class TemaViewModel(
    private val repositorio: TemaRepository,
) : ViewModel() {

    val preferencia: StateFlow<PreferenciaTema> = repositorio.preferencia

    /** The user picks light or dark explicitly; "follow the system" only exists until the first choice. */
    fun onElegirTema(oscuro: Boolean) {
        repositorio.cambiar(if (oscuro) PreferenciaTema.OSCURO else PreferenciaTema.CLARO)
    }
}
