package com.fabio.gestionfacturas.ui.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.clientes_actualizar
import com.fabio.gestionfacturas.resources.clientes_cancelar
import com.fabio.gestionfacturas.resources.clientes_confirmar_desactivar
import com.fabio.gestionfacturas.resources.clientes_confirmar_desactivar_texto
import com.fabio.gestionfacturas.resources.clientes_confirmar_desactivar_titulo
import com.fabio.gestionfacturas.resources.clientes_desactivar
import com.fabio.gestionfacturas.resources.clientes_editar
import com.fabio.gestionfacturas.resources.clientes_email
import com.fabio.gestionfacturas.resources.clientes_inactivo
import com.fabio.gestionfacturas.resources.clientes_nif
import com.fabio.gestionfacturas.resources.clientes_nuevo
import com.fabio.gestionfacturas.resources.clientes_sin_dato
import com.fabio.gestionfacturas.resources.clientes_telefono
import com.fabio.gestionfacturas.resources.clientes_titulo
import com.fabio.gestionfacturas.resources.clientes_vacio
import com.fabio.gestionfacturas.resources.clientes_ya_no_existe
import com.fabio.gestionfacturas.resources.clientes_desactivar_fallido
import com.fabio.gestionfacturas.resources.clientes_actualizar_fallido
import com.fabio.gestionfacturas.ui.common.EstadoCargando
import com.fabio.gestionfacturas.ui.common.EstadoError
import com.fabio.gestionfacturas.ui.common.EstadoVacio
import com.fabio.gestionfacturas.ui.common.mensaje
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private val AnchoMaximo = 900.dp

@Composable
fun ClientesScreen(
    alNuevo: () -> Unit,
    alEditar: (Long) -> Unit,
    resultadoFormulario: ResultadoFormulario? = null,
    alConsumirResultadoFormulario: () -> Unit = {},
    viewModel: ClientesViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // The form left a result for this screen: act on it once, then clear it.
    LaunchedEffect(resultadoFormulario) {
        if (resultadoFormulario != null) {
            viewModel.onVolverDelFormulario(resultadoFormulario)
            alConsumirResultadoFormulario()
        }
    }

    ClientesContent(
        estado = estado,
        alNuevo = alNuevo,
        alEditar = alEditar,
        alRefrescar = viewModel::onRefrescar,
        alPedirDesactivar = viewModel::onPedirDesactivar,
        alCancelarDesactivar = viewModel::onCancelarDesactivar,
        alConfirmarDesactivar = viewModel::onConfirmarDesactivar,
        alConsumirMensaje = viewModel::onMensajeConsumido,
    )
}

@Composable
fun ClientesContent(
    estado: ClientesUiState,
    alNuevo: () -> Unit,
    alEditar: (Long) -> Unit,
    alRefrescar: () -> Unit,
    alPedirDesactivar: (Cliente) -> Unit,
    alCancelarDesactivar: () -> Unit,
    alConfirmarDesactivar: () -> Unit,
    alConsumirMensaje: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val textoMensaje = estado.mensaje?.texto()
    // Own scope: consuming the message changes the effect key, which would cancel a keyed effect
    // (and dismiss the snackbar) before the user could read it.
    val scope = rememberCoroutineScope()

    LaunchedEffect(estado.mensaje) {
        if (textoMensaje != null) {
            alConsumirMensaje()
            scope.launch { snackbarHostState.showSnackbar(textoMensaje) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Box(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = AnchoMaximo).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Cabecera(
                    habilitado = estado.carga !is CargaClientes.Cargando && !estado.refrescando,
                    alNuevo = alNuevo,
                    alRefrescar = alRefrescar,
                )
                if (estado.refrescando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                when (val carga = estado.carga) {
                    CargaClientes.Cargando -> EstadoCargando()
                    CargaClientes.Vacio -> EstadoVacio(
                        mensaje = stringResource(Res.string.clientes_vacio),
                        textoAccion = stringResource(Res.string.clientes_nuevo),
                        alPulsarAccion = alNuevo,
                    )
                    is CargaClientes.Error -> EstadoError(
                        mensaje = carga.error.mensaje(),
                        alReintentar = alRefrescar,
                    )
                    is CargaClientes.Contenido -> ListaClientes(
                        clientes = carga.clientes,
                        alEditar = alEditar,
                        alDesactivar = alPedirDesactivar,
                    )
                }
            }
        }
    }

    val clienteADesactivar = estado.clienteADesactivar
    if (clienteADesactivar != null) {
        DialogoDesactivar(
            cliente = clienteADesactivar,
            desactivando = estado.desactivando,
            alCancelar = alCancelarDesactivar,
            alConfirmar = alConfirmarDesactivar,
        )
    }
}

@Composable
private fun Cabecera(
    habilitado: Boolean,
    alNuevo: () -> Unit,
    alRefrescar: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.clientes_titulo),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = alRefrescar, enabled = habilitado) {
            Text(stringResource(Res.string.clientes_actualizar))
        }
        Button(onClick = alNuevo) { Text(stringResource(Res.string.clientes_nuevo)) }
    }
}

@Composable
private fun ListaClientes(
    clientes: List<Cliente>,
    alEditar: (Long) -> Unit,
    alDesactivar: (Cliente) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(clientes, key = { it.id }) { cliente ->
            FilaCliente(cliente = cliente, alEditar = alEditar, alDesactivar = alDesactivar)
        }
    }
}

@Composable
private fun FilaCliente(
    cliente: Cliente,
    alEditar: (Long) -> Unit,
    alDesactivar: (Cliente) -> Unit,
) {
    val sinDato = stringResource(Res.string.clientes_sin_dato)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = cliente.nombre, style = MaterialTheme.typography.titleMedium)
                    if (!cliente.activo) {
                        Text(
                            text = stringResource(Res.string.clientes_inactivo),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text(stringResource(Res.string.clientes_nif) + ": " + cliente.nif)
                Text(stringResource(Res.string.clientes_email) + ": " + (cliente.email ?: sinDato))
                Text(stringResource(Res.string.clientes_telefono) + ": " + (cliente.telefono ?: sinDato))
            }
            OutlinedButton(onClick = { alEditar(cliente.id) }) {
                Text(stringResource(Res.string.clientes_editar))
            }
            if (cliente.activo) {
                TextButton(onClick = { alDesactivar(cliente) }) {
                    Text(stringResource(Res.string.clientes_desactivar))
                }
            }
        }
    }
}

@Composable
private fun DialogoDesactivar(
    cliente: Cliente,
    desactivando: Boolean,
    alCancelar: () -> Unit,
    alConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.clientes_confirmar_desactivar_titulo)) },
        text = { Text(stringResource(Res.string.clientes_confirmar_desactivar_texto, cliente.nombre)) },
        confirmButton = {
            Button(onClick = alConfirmar, enabled = !desactivando) {
                Text(stringResource(Res.string.clientes_confirmar_desactivar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, enabled = !desactivando) {
                Text(stringResource(Res.string.clientes_cancelar))
            }
        },
    )
}

/** The text for a one-shot message; the error detail comes from the shared error texts. */
@Composable
private fun MensajeClientes.texto(): String {
    return when (this) {
        MensajeClientes.YaNoExiste -> stringResource(Res.string.clientes_ya_no_existe)
        is MensajeClientes.DesactivarFallido ->
            stringResource(Res.string.clientes_desactivar_fallido) + " " + error.mensaje()
        is MensajeClientes.ActualizarFallido ->
            stringResource(Res.string.clientes_actualizar_fallido) + " " + error.mensaje()
    }
}
