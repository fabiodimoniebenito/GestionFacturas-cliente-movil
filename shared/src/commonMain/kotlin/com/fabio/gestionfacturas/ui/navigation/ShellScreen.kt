package com.fabio.gestionfacturas.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.editar_cliente_titulo
import com.fabio.gestionfacturas.resources.menu_app_titulo
import com.fabio.gestionfacturas.resources.menu_cerrar_sesion
import com.fabio.gestionfacturas.resources.menu_clientes
import com.fabio.gestionfacturas.resources.menu_dashboard
import com.fabio.gestionfacturas.resources.menu_gastos
import com.fabio.gestionfacturas.resources.menu_ingresos
import com.fabio.gestionfacturas.resources.menu_proximamente
import com.fabio.gestionfacturas.resources.nuevo_cliente_titulo
import com.fabio.gestionfacturas.resources.pendiente_parte_3
import com.fabio.gestionfacturas.ui.common.BarraTitulo
import com.fabio.gestionfacturas.ui.clientes.ClientesScreen
import com.fabio.gestionfacturas.ui.common.BotonPrincipal
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Logged-in layout: left sidebar plus a content area that hosts its own navigation. */
@Composable
fun ShellScreen(
    viewModel: ShellViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Row(modifier = Modifier.fillMaxSize()) {
        BarraLateral(
            email = estado.email,
            cerrandoSesion = estado.cerrandoSesion,
            alCerrarSesion = viewModel::onCerrarSesion,
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            ContenidoShell(navController)
        }
    }
}

@Composable
private fun BarraLateral(
    email: String,
    cerrandoSesion: Boolean,
    alCerrarSesion: () -> Unit,
) {
    PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.menu_app_titulo),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(12.dp),
            )
            // Clientes is the only area built so far; its inner routes all belong to it.
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.menu_clientes)) },
                selected = true,
                onClick = {},
            )
            ElementoPendiente(stringResource(Res.string.menu_gastos))
            ElementoPendiente(stringResource(Res.string.menu_ingresos))
            ElementoPendiente(stringResource(Res.string.menu_dashboard))
            Spacer(Modifier.weight(1f))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            BotonPrincipal(
                texto = stringResource(Res.string.menu_cerrar_sesion),
                cargando = cerrandoSesion,
                alPulsar = alCerrarSesion,
            )
        }
    }
}

/** An area that does not exist yet: visible but not usable. */
@Composable
private fun ElementoPendiente(nombre: String) {
    NavigationDrawerItem(
        label = { Text(nombre + " (" + stringResource(Res.string.menu_proximamente) + ")") },
        selected = false,
        onClick = {},
        modifier = Modifier.alpha(0.38f).semantics { disabled() },
    )
}

@Composable
private fun ContenidoShell(navController: NavHostController) {
    NavHost(navController = navController, startDestination = RutaClientes) {
        composable<RutaClientes> {
            ClientesScreen(
                alNuevo = { navController.navigate(RutaNuevoCliente) },
                alEditar = { id -> navController.navigate(RutaEditarCliente(id)) },
            )
        }
        composable<RutaNuevoCliente> {
            PantallaProvisional(
                titulo = stringResource(Res.string.nuevo_cliente_titulo),
                alVolver = { navController.popBackStack() },
            )
        }
        composable<RutaEditarCliente> { entrada ->
            val ruta = entrada.toRoute<RutaEditarCliente>()
            PantallaProvisional(
                titulo = stringResource(Res.string.editar_cliente_titulo) + " #" + ruta.id,
                alVolver = { navController.popBackStack() },
            )
        }
    }
}

/** Stub for screens that a later part implements. */
@Composable
private fun PantallaProvisional(titulo: String, alVolver: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BarraTitulo(titulo = titulo, alVolver = alVolver)
        Text(stringResource(Res.string.pendiente_parte_3))
    }
}
