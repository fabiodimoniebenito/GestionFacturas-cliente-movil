package com.fabio.gestionfacturas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.ui.navigation.AppNavigation
import com.fabio.gestionfacturas.ui.navigation.TemaViewModel
import com.fabio.gestionfacturas.ui.navigation.esOscuro
import com.fabio.gestionfacturas.ui.theme.TemaGestionFacturas
import org.koin.compose.viewmodel.koinViewModel

/** Root composable. Koin must already be started with `initKoin()` by the platform entry point. */
@Composable
fun App(temaViewModel: TemaViewModel = koinViewModel()) {
    val preferencia by temaViewModel.preferencia.collectAsStateWithLifecycle()
    val oscuro = preferencia.esOscuro(isSystemInDarkTheme())
    TemaGestionFacturas(oscuro = oscuro) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation()
        }
    }
}
