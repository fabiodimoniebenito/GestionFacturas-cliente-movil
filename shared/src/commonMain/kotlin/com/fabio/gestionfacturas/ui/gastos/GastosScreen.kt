package com.fabio.gestionfacturas.ui.gastos

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.gastos_actualizar
import com.fabio.gestionfacturas.resources.gastos_actualizar_fallido
import com.fabio.gestionfacturas.resources.gastos_base_imponible
import com.fabio.gestionfacturas.resources.gastos_iva
import com.fabio.gestionfacturas.resources.gastos_pendiente_revision
import com.fabio.gestionfacturas.resources.gastos_sin_dato
import com.fabio.gestionfacturas.resources.gastos_sin_emisor
import com.fabio.gestionfacturas.resources.gastos_titulo
import com.fabio.gestionfacturas.resources.gastos_vacio
import com.fabio.gestionfacturas.ui.common.EstadoCargando
import com.fabio.gestionfacturas.ui.common.EstadoError
import com.fabio.gestionfacturas.ui.common.EstadoVacio
import com.fabio.gestionfacturas.ui.common.mensaje
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val AnchoMaximo = 900.dp

@Composable
fun GastosScreen(
    alAbrir: (Long) -> Unit,
    viewModel: GastosViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    GastosContent(
        estado = estado,
        alAbrir = alAbrir,
        alRefrescar = viewModel::onRefrescar,
        alConsumirMensaje = viewModel::onMensajeConsumido,
    )
}

@Composable
fun GastosContent(
    estado: GastosUiState,
    alAbrir: (Long) -> Unit,
    alRefrescar: () -> Unit,
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
                    habilitado = estado.carga !is CargaGastos.Cargando && !estado.refrescando,
                    alRefrescar = alRefrescar,
                )
                if (estado.refrescando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                when (val carga = estado.carga) {
                    CargaGastos.Cargando -> EstadoCargando()
                    CargaGastos.Vacio -> EstadoVacio(mensaje = stringResource(Res.string.gastos_vacio))
                    is CargaGastos.Error -> EstadoError(mensaje = carga.error.mensaje(), alReintentar = alRefrescar)
                    is CargaGastos.Contenido -> ListaGastos(gastos = carga.gastos, alAbrir = alAbrir)
                }
            }
        }
    }
}

@Composable
private fun Cabecera(habilitado: Boolean, alRefrescar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.gastos_titulo),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = alRefrescar, enabled = habilitado) {
            Text(stringResource(Res.string.gastos_actualizar))
        }
    }
}

@Composable
private fun ListaGastos(gastos: List<Gasto>, alAbrir: (Long) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(gastos, key = { it.id }) { gasto -> FilaGasto(gasto = gasto, alAbrir = alAbrir) }
    }
}

@Composable
private fun FilaGasto(gasto: Gasto, alAbrir: (Long) -> Unit) {
    val sinDato = stringResource(Res.string.gastos_sin_dato)
    val pendiente = gasto.estado == EstadoGasto.BORRADOR
    val colores = if (pendiente) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    } else {
        CardDefaults.cardColors()
    }
    Card(onClick = { alAbrir(gasto.id) }, colors = colores, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = gasto.emisor?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.gastos_sin_emisor),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(gasto.fechaEmision?.toString() ?: sinDato)
                Text(
                    stringResource(Res.string.gastos_base_imponible) + ": " +
                        textoImporte(gasto.baseImponible, gasto.moneda),
                )
                Text(stringResource(Res.string.gastos_iva) + ": " + textoImporte(gasto.iva, gasto.moneda))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EtiquetaEstado(gasto.estado)
                if (pendiente) {
                    Text(
                        text = stringResource(Res.string.gastos_pendiente_revision),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/** The text for a one-shot message; the error detail comes from the shared error texts. */
@Composable
private fun MensajeGastos.texto(): String = when (this) {
    is MensajeGastos.ActualizarFallido -> stringResource(Res.string.gastos_actualizar_fallido) + " " + error.mensaje()
}
