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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.menu_app_titulo
import com.fabio.gestionfacturas.resources.menu_cerrar_sesion
import com.fabio.gestionfacturas.resources.menu_clientes
import com.fabio.gestionfacturas.resources.menu_dashboard
import com.fabio.gestionfacturas.resources.menu_gastos
import com.fabio.gestionfacturas.resources.menu_ingresos
import com.fabio.gestionfacturas.resources.menu_proximamente
import com.fabio.gestionfacturas.ui.clientes.ClienteFormScreen
import com.fabio.gestionfacturas.ui.clientes.ClientesScreen
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.common.BotonPrincipal
import com.fabio.gestionfacturas.ui.gastos.GastoDetalleScreen
import com.fabio.gestionfacturas.ui.gastos.GastosScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Logged-in layout: left sidebar plus a content area that hosts its own navigation. */
@Composable
fun ShellScreen(
    viewModel: ShellViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val destino = navController.currentBackStackEntryAsState().value?.destination
    val enGastos = destino?.hasRoute<RutaGastos>() == true || destino?.hasRoute<RutaDetalleGasto>() == true
    val enClientes = destino?.hasRoute<RutaClientes>() == true ||
        destino?.hasRoute<RutaNuevoCliente>() == true ||
        destino?.hasRoute<RutaEditarCliente>() == true

    Row(modifier = Modifier.fillMaxSize()) {
        BarraLateral(
            enClientes = enClientes,
            enGastos = enGastos,
            alIrAClientes = { irA(navController, RutaClientes) },
            alIrAGastos = { irA(navController, RutaGastos) },
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
    enClientes: Boolean,
    enGastos: Boolean,
    alIrAClientes: () -> Unit,
    alIrAGastos: () -> Unit,
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
            // Each area owns its inner routes (list, form, detail), so the item stays selected inside them.
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.menu_clientes)) },
                selected = enClientes,
                onClick = alIrAClientes,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.menu_gastos)) },
                selected = enGastos,
                onClick = alIrAGastos,
            )
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

/** Switches area: the back stack returns to the start so areas do not pile up. */
private fun irA(navController: NavHostController, ruta: Any) {
    navController.navigate(ruta) {
        popUpTo(RutaClientes) { inclusive = false }
        launchSingleTop = true
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

/** Key under which the form leaves its result for the list (read once, then cleared). */
private const val CLAVE_RESULTADO_FORMULARIO = "resultadoFormulario"

@Composable
private fun ContenidoShell(navController: NavHostController) {
    val terminarFormulario: (ResultadoFormulario) -> Unit = { resultado ->
        navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_RESULTADO_FORMULARIO, resultado.name)
        navController.popBackStack()
    }

    NavHost(navController = navController, startDestination = RutaClientes) {
        composable<RutaClientes> { entrada ->
            val resultado by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_RESULTADO_FORMULARIO, null)
                .collectAsStateWithLifecycle()
            ClientesScreen(
                alNuevo = { navController.navigate(RutaNuevoCliente) },
                alEditar = { id -> navController.navigate(RutaEditarCliente(id)) },
                resultadoFormulario = resultado?.let { ResultadoFormulario.valueOf(it) },
                alConsumirResultadoFormulario = { entrada.savedStateHandle[CLAVE_RESULTADO_FORMULARIO] = null },
            )
        }
        composable<RutaGastos> {
            GastosScreen(alAbrir = { id -> navController.navigate(RutaDetalleGasto(id)) })
        }
        composable<RutaDetalleGasto> { entrada ->
            val ruta = entrada.toRoute<RutaDetalleGasto>()
            GastoDetalleScreen(idGasto = ruta.id, alVolver = { navController.popBackStack() })
        }
        composable<RutaNuevoCliente> {
            ClienteFormScreen(
                idCliente = null,
                alVolver = { navController.popBackStack() },
                alTerminar = terminarFormulario,
            )
        }
        composable<RutaEditarCliente> { entrada ->
            val ruta = entrada.toRoute<RutaEditarCliente>()
            ClienteFormScreen(
                idCliente = ruta.id,
                alVolver = { navController.popBackStack() },
                alTerminar = terminarFormulario,
            )
        }
    }
}
