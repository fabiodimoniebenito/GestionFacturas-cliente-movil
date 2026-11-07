package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.PreferenciaTema
import kotlinx.coroutines.flow.StateFlow

interface TemaRepository {
    val preferencia: StateFlow<PreferenciaTema>

    fun cambiar(preferencia: PreferenciaTema)
}
