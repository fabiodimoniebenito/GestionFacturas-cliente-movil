package com.fabio.gestionfacturas.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.fabio.gestionfacturas.resources.menu_tema_a_claro
import com.fabio.gestionfacturas.resources.menu_tema_a_oscuro
import com.fabio.gestionfacturas.ui.clientes.ClienteFormScreen
import com.fabio.gestionfacturas.ui.clientes.ClientesScreen
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.dashboard.DashboardScreen
import com.fabio.gestionfacturas.ui.gastos.DigitalizarScreen
import com.fabio.gestionfacturas.ui.gastos.GastoDetalleScreen
import com.fabio.gestionfacturas.ui.gastos.GastoFormScreen
import com.fabio.gestionfacturas.ui.gastos.GastosScreen
import com.fabio.gestionfacturas.ui.ingresos.IngresoDetalleScreen
import com.fabio.gestionfacturas.ui.ingresos.IngresoFormScreen
import com.fabio.gestionfacturas.ui.ingresos.IngresosScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Logged-in layout: top bar plus a content area that hosts its own navigation. */
@Composable
fun ShellScreen(
    viewModel: ShellViewModel = koinViewModel(),
    temaViewModel: TemaViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val tema by temaViewModel.preferencia.collectAsStateWithLifecycle()
    val oscuro = tema.esOscuro(isSystemInDarkTheme())
    val navController = rememberNavController()
    val destino = navController.currentBackStackEntryAsState().value?.destination
    val enDashboard = destino?.hasRoute<RutaDashboard>() == true
    val enGastos = destino?.hasRoute<RutaGastos>() == true || destino?.hasRoute<RutaDetalleGasto>() == true ||
        destino?.hasRoute<RutaDigitalizarGasto>() == true
    val enIngresos = destino?.hasRoute<RutaIngresos>() == true ||
        destino?.hasRoute<RutaNuevoIngreso>() == true ||
        destino?.hasRoute<RutaDetalleIngreso>() == true
    val enClientes = destino?.hasRoute<RutaClientes>() == true ||
        destino?.hasRoute<RutaNuevoCliente>() == true ||
        destino?.hasRoute<RutaEditarCliente>() == true

    Column(modifier = Modifier.fillMaxSize()) {
        BarraSuperior(
            enDashboard = enDashboard,
            enClientes = enClientes,
            enGastos = enGastos,
            enIngresos = enIngresos,
            alIrAlDashboard = { irA(navController, RutaDashboard) },
            alIrAClientes = { irA(navController, RutaClientes) },
            alIrAGastos = { irA(navController, RutaGastos) },
            alIrAIngresos = { irA(navController, RutaIngresos) },
            oscuro = oscuro,
            alCambiarTema = { temaViewModel.onElegirTema(!oscuro) },
            email = estado.email,
            cerrandoSesion = estado.cerrandoSesion,
            alCerrarSesion = viewModel::onCerrarSesion,
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ContenidoShell(navController)
        }
    }
}

/** Top bar of the logged-in layout: app name, the four areas as pills, then theme toggle and session. */
@Composable
private fun BarraSuperior(
    enDashboard: Boolean,
    enClientes: Boolean,
    enGastos: Boolean,
    enIngresos: Boolean,
    alIrAlDashboard: () -> Unit,
    alIrAClientes: () -> Unit,
    alIrAGastos: () -> Unit,
    alIrAIngresos: () -> Unit,
    oscuro: Boolean,
    alCambiarTema: () -> Unit,
    email: String,
    cerrandoSesion: Boolean,
    alCerrarSesion: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.menu_app_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        // Each area owns its inner routes (list, form, detail), so its pill stays selected inside them.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PildoraNavegacion(stringResource(Res.string.menu_dashboard), enDashboard, alIrAlDashboard)
            PildoraNavegacion(stringResource(Res.string.menu_clientes), enClientes, alIrAClientes)
            PildoraNavegacion(stringResource(Res.string.menu_gastos), enGastos, alIrAGastos)
            PildoraNavegacion(stringResource(Res.string.menu_ingresos), enIngresos, alIrAIngresos)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = alCambiarTema) {
            // The icon shows the current mode; the description says what pressing it does.
            Icon(
                imageVector = if (oscuro) IconoLuna else IconoSol,
                contentDescription = stringResource(
                    if (oscuro) Res.string.menu_tema_a_claro else Res.string.menu_tema_a_oscuro,
                ),
            )
        }
        TextButton(onClick = alCerrarSesion, enabled = !cerrandoSesion) {
            Text(stringResource(Res.string.menu_cerrar_sesion))
        }
    }
}

@Composable
private fun PildoraNavegacion(texto: String, seleccionada: Boolean, alPulsar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Surface(
        onClick = alPulsar,
        shape = CircleShape,
        color = if (seleccionada) colores.surfaceVariant else Color.Transparent,
        contentColor = if (seleccionada) colores.onSurface else colores.onSurfaceVariant,
        modifier = Modifier.semantics { selected = seleccionada },
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/** Switches area: the back stack returns to the start so areas do not pile up. */
private fun irA(navController: NavHostController, ruta: Any) {
    navController.navigate(ruta) {
        popUpTo(RutaDashboard) { inclusive = false }
        launchSingleTop = true
    }
}

/** Key under which the form leaves its result for the list (read once, then cleared). */
private const val CLAVE_RESULTADO_FORMULARIO = "resultadoFormulario"

/** Leaves [resultado] for the screen below the current one (the gastos list). */
private fun avisarALaLista(navController: NavHostController, resultado: ResultadoFormulario) {
    navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_RESULTADO_FORMULARIO, resultado.name)
}

@Composable
private fun ContenidoShell(navController: NavHostController) {
    val terminarFormulario: (ResultadoFormulario) -> Unit = { resultado ->
        navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_RESULTADO_FORMULARIO, resultado.name)
        navController.popBackStack()
    }

    NavHost(navController = navController, startDestination = RutaDashboard) {
        composable<RutaDashboard> { entrada ->
            // A cobro registered or reverted in the detail leaves a result here: the figures are stale.
            val resultado by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_RESULTADO_FORMULARIO, null)
                .collectAsStateWithLifecycle()
            DashboardScreen(
                alAbrirIngreso = { id -> navController.navigate(RutaDetalleIngreso(id)) },
                volvioDelDetalle = resultado != null,
                alConsumirVueltaDelDetalle = { entrada.savedStateHandle[CLAVE_RESULTADO_FORMULARIO] = null },
            )
        }
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
        composable<RutaGastos> { entrada ->
            val resultado by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_RESULTADO_FORMULARIO, null)
                .collectAsStateWithLifecycle()
            GastosScreen(
                alNuevo = { navController.navigate(RutaNuevoGasto) },
                alDigitalizar = { navController.navigate(RutaDigitalizarGasto) },
                alAbrir = { id -> navController.navigate(RutaDetalleGasto(id)) },
                resultadoFormulario = resultado?.let { ResultadoFormulario.valueOf(it) },
                alConsumirResultadoFormulario = { entrada.savedStateHandle[CLAVE_RESULTADO_FORMULARIO] = null },
            )
        }
        composable<RutaDigitalizarGasto> {
            DigitalizarScreen(
                alVolver = { navController.popBackStack() },
                alCrearGasto = { id ->
                    // The list below must refresh; then this screen is replaced by the edit form of the new gasto.
                    avisarALaLista(navController, ResultadoFormulario.GUARDADO)
                    navController.navigate(RutaEditarGasto(id, revisarDatosExtraidos = true)) {
                        popUpTo<RutaDigitalizarGasto> { inclusive = true }
                    }
                },
                alCrearManual = {
                    navController.navigate(RutaNuevoGasto) {
                        popUpTo<RutaDigitalizarGasto> { inclusive = true }
                    }
                },
            )
        }
        composable<RutaDetalleGasto> { entrada ->
            val ruta = entrada.toRoute<RutaDetalleGasto>()
            val resultado by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_RESULTADO_FORMULARIO, null)
                .collectAsStateWithLifecycle()
            GastoDetalleScreen(
                idGasto = ruta.id,
                alVolver = { navController.popBackStack() },
                alEditar = { navController.navigate(RutaEditarGasto(ruta.id)) },
                resultadoFormulario = resultado?.let { ResultadoFormulario.valueOf(it) },
                alConsumirResultadoFormulario = { entrada.savedStateHandle[CLAVE_RESULTADO_FORMULARIO] = null },
                // The list below must refresh after a save or a review made from here.
                alHuboCambios = { avisarALaLista(navController, ResultadoFormulario.GUARDADO) },
                // The gasto is gone: back to the list, which explains it.
                alYaNoExiste = {
                    avisarALaLista(navController, ResultadoFormulario.NO_ENCONTRADO)
                    navController.popBackStack()
                },
            )
        }
        composable<RutaIngresos> { entrada ->
            val resultado by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_RESULTADO_FORMULARIO, null)
                .collectAsStateWithLifecycle()
            IngresosScreen(
                alNuevo = { navController.navigate(RutaNuevoIngreso) },
                alAbrir = { id -> navController.navigate(RutaDetalleIngreso(id)) },
                resultadoDetalle = resultado?.let { ResultadoFormulario.valueOf(it) },
                alConsumirResultadoDetalle = { entrada.savedStateHandle[CLAVE_RESULTADO_FORMULARIO] = null },
            )
        }
        composable<RutaNuevoIngreso> { entrada ->
            // Coming back from creating a cliente (the shortcut of the empty selector) leaves a result here.
            val resultado by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_RESULTADO_FORMULARIO, null)
                .collectAsStateWithLifecycle()
            IngresoFormScreen(
                alVolver = { navController.popBackStack() },
                alCrearCliente = { navController.navigate(RutaNuevoCliente) },
                alCrear = { id ->
                    // The list below must refresh; then the form is replaced by the detail of the new ingreso.
                    avisarALaLista(navController, ResultadoFormulario.GUARDADO)
                    navController.navigate(RutaDetalleIngreso(id)) {
                        popUpTo<RutaNuevoIngreso> { inclusive = true }
                    }
                },
                clienteCreado = resultado == ResultadoFormulario.GUARDADO.name,
                alConsumirClienteCreado = { entrada.savedStateHandle[CLAVE_RESULTADO_FORMULARIO] = null },
            )
        }
        composable<RutaDetalleIngreso> { entrada ->
            val ruta = entrada.toRoute<RutaDetalleIngreso>()
            IngresoDetalleScreen(
                idIngreso = ruta.id,
                alVolver = { navController.popBackStack() },
                // The list below must refresh after a cobro made from here.
                alHuboCambios = { avisarALaLista(navController, ResultadoFormulario.GUARDADO) },
                // The ingreso is gone: back to the list, which explains it.
                alYaNoExiste = {
                    avisarALaLista(navController, ResultadoFormulario.NO_ENCONTRADO)
                    navController.popBackStack()
                },
            )
        }
        composable<RutaNuevoGasto> {
            GastoFormScreen(
                idGasto = null,
                alVolver = { navController.popBackStack() },
                alTerminar = terminarFormulario,
            )
        }
        composable<RutaEditarGasto> { entrada ->
            val ruta = entrada.toRoute<RutaEditarGasto>()
            GastoFormScreen(
                idGasto = ruta.id,
                alVolver = { navController.popBackStack() },
                alTerminar = terminarFormulario,
                mostrarAvisoRevision = ruta.revisarDatosExtraidos,
            )
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
