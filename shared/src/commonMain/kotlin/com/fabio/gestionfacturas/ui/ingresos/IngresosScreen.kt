package com.fabio.gestionfacturas.ui.ingresos

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
import androidx.compose.material3.FilterChip
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
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.ingresos_actualizar
import com.fabio.gestionfacturas.resources.ingresos_actualizar_fallido
import com.fabio.gestionfacturas.resources.ingresos_cliente_desconocido
import com.fabio.gestionfacturas.resources.ingresos_filtro_cobrados
import com.fabio.gestionfacturas.resources.ingresos_filtro_pendientes
import com.fabio.gestionfacturas.resources.ingresos_filtro_todos
import com.fabio.gestionfacturas.resources.ingresos_sin_cliente
import com.fabio.gestionfacturas.resources.ingresos_sin_concepto
import com.fabio.gestionfacturas.resources.ingresos_sin_dato
import com.fabio.gestionfacturas.resources.ingresos_titulo
import com.fabio.gestionfacturas.resources.ingresos_total
import com.fabio.gestionfacturas.resources.ingresos_vacio
import com.fabio.gestionfacturas.resources.ingresos_vacio_filtro
import com.fabio.gestionfacturas.resources.ingresos_ya_no_existe
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.common.EstadoCargando
import com.fabio.gestionfacturas.ui.common.EstadoError
import com.fabio.gestionfacturas.ui.common.EstadoVacio
import com.fabio.gestionfacturas.ui.common.mensaje
import com.fabio.gestionfacturas.ui.gastos.textoImporte
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val AnchoMaximo = 900.dp

@Composable
fun IngresosScreen(
    alAbrir: (Long) -> Unit,
    resultadoDetalle: ResultadoFormulario? = null,
    alConsumirResultadoDetalle: () -> Unit = {},
    viewModel: IngresosViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // The detail left a result for this screen: act on it once, then clear it.
    LaunchedEffect(resultadoDetalle) {
        if (resultadoDetalle != null) {
            viewModel.onVolverDelDetalle(resultadoDetalle)
            alConsumirResultadoDetalle()
        }
    }
    IngresosContent(
        estado = estado,
        alAbrir = alAbrir,
        alRefrescar = viewModel::onRefrescar,
        alCambiarFiltro = viewModel::onFiltroCambiado,
        alConsumirMensaje = viewModel::onMensajeConsumido,
    )
}

@Composable
fun IngresosContent(
    estado: IngresosUiState,
    alAbrir: (Long) -> Unit,
    alRefrescar: () -> Unit,
    alCambiarFiltro: (FiltroIngresos) -> Unit,
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
                    habilitado = estado.carga !is CargaIngresos.Cargando && !estado.refrescando,
                    alRefrescar = alRefrescar,
                )
                if (estado.refrescando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                when (val carga = estado.carga) {
                    CargaIngresos.Cargando -> EstadoCargando()
                    CargaIngresos.Vacio -> EstadoVacio(mensaje = stringResource(Res.string.ingresos_vacio))
                    is CargaIngresos.Error -> EstadoError(mensaje = carga.error.mensaje(), alReintentar = alRefrescar)
                    is CargaIngresos.Contenido -> {
                        Filtros(filtro = estado.filtro, alCambiar = alCambiarFiltro)
                        val visibles = estado.visibles
                        if (visibles.isEmpty()) {
                            EstadoVacio(mensaje = stringResource(Res.string.ingresos_vacio_filtro))
                        } else {
                            ListaIngresos(filas = visibles, alAbrir = alAbrir)
                        }
                    }
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
            text = stringResource(Res.string.ingresos_titulo),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = alRefrescar, enabled = habilitado) {
            Text(stringResource(Res.string.ingresos_actualizar))
        }
    }
}

@Composable
private fun Filtros(filtro: FiltroIngresos, alCambiar: (FiltroIngresos) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FiltroIngresos.entries.forEach { opcion ->
            FilterChip(
                selected = filtro == opcion,
                onClick = { alCambiar(opcion) },
                label = { Text(opcion.texto()) },
            )
        }
    }
}

@Composable
private fun FiltroIngresos.texto(): String = when (this) {
    FiltroIngresos.TODOS -> stringResource(Res.string.ingresos_filtro_todos)
    FiltroIngresos.PENDIENTES -> stringResource(Res.string.ingresos_filtro_pendientes)
    FiltroIngresos.COBRADOS -> stringResource(Res.string.ingresos_filtro_cobrados)
}

@Composable
private fun ListaIngresos(filas: List<FilaIngreso>, alAbrir: (Long) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filas, key = { it.ingreso.id }) { fila -> FilaIngresoCard(fila = fila, alAbrir = alAbrir) }
    }
}

@Composable
private fun FilaIngresoCard(fila: FilaIngreso, alAbrir: (Long) -> Unit) {
    val ingreso = fila.ingreso
    val pendiente = ingreso.estadoCobro == EstadoCobro.PENDIENTE
    val colores = if (pendiente) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    } else {
        CardDefaults.cardColors()
    }
    val cliente = when {
        fila.nombreCliente != null -> fila.nombreCliente
        ingreso.clienteId == null -> stringResource(Res.string.ingresos_sin_cliente)
        else -> stringResource(Res.string.ingresos_cliente_desconocido)
    }
    Card(onClick = { alAbrir(ingreso.id) }, colors = colores, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = ingreso.concepto?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.ingresos_sin_concepto),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(cliente)
                Text(ingreso.fechaEmision?.toString() ?: stringResource(Res.string.ingresos_sin_dato))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EtiquetaEstadoCobro(ingreso.estadoCobro)
                Text(
                    text = stringResource(Res.string.ingresos_total) + ": " + textoImporte(ingreso.total, ingreso.moneda),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

/** The text for a one-shot message; the error detail comes from the shared error texts. */
@Composable
private fun MensajeIngresos.texto(): String = when (this) {
    MensajeIngresos.YaNoExiste -> stringResource(Res.string.ingresos_ya_no_existe)
    is MensajeIngresos.ActualizarFallido -> stringResource(Res.string.ingresos_actualizar_fallido) + " " + error.mensaje()
}
