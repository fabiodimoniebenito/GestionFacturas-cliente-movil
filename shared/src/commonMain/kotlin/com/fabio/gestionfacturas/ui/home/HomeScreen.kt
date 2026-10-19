package com.fabio.gestionfacturas.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.home_cerrar_sesion
import com.fabio.gestionfacturas.resources.home_sesion_de
import com.fabio.gestionfacturas.resources.home_titulo
import com.fabio.gestionfacturas.ui.common.BotonPrincipal
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Placeholder for the logged-in area: shows the email and a logout button. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.home_titulo),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(stringResource(Res.string.home_sesion_de, estado.email))
        BotonPrincipal(
            texto = stringResource(Res.string.home_cerrar_sesion),
            cargando = estado.cerrandoSesion,
            alPulsar = viewModel::onCerrarSesion,
        )
    }
}
