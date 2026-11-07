package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.domain.model.PreferenciaTema
import com.fabio.gestionfacturas.domain.repository.TemaRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps the theme choice in Settings; an unknown or missing value means [PreferenciaTema.SISTEMA]. */
class TemaRepositoryImpl(private val settings: Settings) : TemaRepository {

    private val _preferencia = MutableStateFlow(leer())
    override val preferencia: StateFlow<PreferenciaTema> = _preferencia.asStateFlow()

    override fun cambiar(preferencia: PreferenciaTema) {
        settings.putString(CLAVE, preferencia.name)
        _preferencia.value = preferencia
    }

    private fun leer(): PreferenciaTema =
        PreferenciaTema.entries.firstOrNull { it.name == settings.getStringOrNull(CLAVE) }
            ?: PreferenciaTema.SISTEMA

    private companion object {
        const val CLAVE = "ui.tema"
    }
}
