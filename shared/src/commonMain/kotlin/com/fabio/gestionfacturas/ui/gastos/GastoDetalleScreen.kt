package com.fabio.gestionfacturas.ui.gastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
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
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.gasto_detalle_base_imponible
import com.fabio.gestionfacturas.resources.gasto_detalle_categoria
import com.fabio.gestionfacturas.resources.gasto_detalle_cliente
import com.fabio.gestionfacturas.resources.gasto_detalle_creado_en
import com.fabio.gestionfacturas.resources.gasto_detalle_deducible
import com.fabio.gestionfacturas.resources.gasto_detalle_editar
import com.fabio.gestionfacturas.resources.gasto_detalle_emisor
import com.fabio.gestionfacturas.resources.gasto_detalle_estado
import com.fabio.gestionfacturas.resources.gasto_detalle_fecha_emision
import com.fabio.gestionfacturas.resources.gasto_detalle_id
import com.fabio.gestionfacturas.resources.gasto_detalle_iva
import com.fabio.gestionfacturas.resources.gasto_detalle_moneda
import com.fabio.gestionfacturas.resources.gasto_detalle_no
import com.fabio.gestionfacturas.resources.gasto_detalle_revisar
import com.fabio.gestionfacturas.resources.gasto_detalle_si
import com.fabio.gestionfacturas.resources.gasto_detalle_titulo
import com.fabio.gestionfacturas.resources.gasto_detalle_ya_no_existe
import com.fabio.gestionfacturas.resources.gasto_revisar_cancelar
import com.fabio.gestionfacturas.resources.gasto_revisar_conflicto
import com.fabio.gestionfacturas.resources.gasto_revisar_confirmar
import com.fabio.gestionfacturas.resources.gasto_revisar_falta_datos
import com.fabio.gestionfacturas.resources.gasto_revisar_fallido
import com.fabio.gestionfacturas.resources.gasto_revisar_ok
import com.fabio.gestionfacturas.resources.gasto_revisar_texto
import com.fabio.gestionfacturas.resources.gasto_revisar_titulo
import com.fabio.gestionfacturas.resources.gastos_sin_dato
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.common.BarraTitulo
import com.fabio.gestionfacturas.ui.common.EstadoCargando
import com.fabio.gestionfacturas.ui.common.EstadoError
import com.fabio.gestionfacturas.ui.common.EstadoVacio
import com.fabio.gestionfacturas.ui.common.mensaje
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val AnchoMaximo = 900.dp

@Composable
fun GastoDetalleScreen(
    idGasto: Long,
    alVolver: () -> Unit,
    alEditar: () -> Unit,
    resultadoFormulario: ResultadoFormulario?,
    alConsumirResultadoFormulario: () -> Unit,
    alHuboCambios: () -> Unit,
    alYaNoExiste: () -> Unit,
    viewModel: GastoDetalleViewModel = koinViewModel(parameters = { parametersOf(idGasto) }),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // The edit form left a result for this screen: act on it once, then clear it.
    LaunchedEffect(resultadoFormulario) {
        when (resultadoFormulario) {
            ResultadoFormulario.GUARDADO -> {
                viewModel.onRecargar()
                alHuboCambios()
            }
            ResultadoFormulario.NO_ENCONTRADO -> alYaNoExiste()
            null -> return@LaunchedEffect
        }
        alConsumirResultadoFormulario()
    }
    GastoDetalleContent(
        estado = estado,
        alVolver = alVolver,
        alReintentar = viewModel::onReintentar,
        alEditar = alEditar,
        alPedirRevisar = viewModel::onPedirRevisar,
        alCancelarRevisar = viewModel::onCancelarRevisar,
        alConfirmarRevisar = viewModel::onConfirmarRevisar,
        alConsumirMensaje = viewModel::onMensajeConsumido,
    )
    // A review succeeded: the list below must refresh.
    LaunchedEffect(estado.huboCambios) {
        if (estado.huboCambios) {
            viewModel.onCambiosNotificados()
            alHuboCambios()
        }
    }
}

@Composable
fun GastoDetalleContent(
    estado: GastoDetalleUiState,
    alVolver: () -> Unit,
    alReintentar: () -> Unit,
    alEditar: () -> Unit,
    alPedirRevisar: () -> Unit,
    alCancelarRevisar: () -> Unit,
    alConfirmarRevisar: () -> Unit,
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
        Box(modifier = Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = AnchoMaximo).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BarraTitulo(titulo = stringResource(Res.string.gasto_detalle_titulo), alVolver = alVolver)
                when (val carga = estado.carga) {
                    CargaGastoDetalle.Cargando -> EstadoCargando()
                    CargaGastoDetalle.NoEncontrado ->
                        EstadoVacio(mensaje = stringResource(Res.string.gasto_detalle_ya_no_existe))
                    is CargaGastoDetalle.Error -> EstadoError(mensaje = carga.error.mensaje(), alReintentar = alReintentar)
                    is CargaGastoDetalle.Contenido -> Contenido(
                        gasto = carga.gasto,
                        nombreCategoria = carga.nombreCategoria,
                        puedeEditar = estado.puedeEditar,
                        puedeRevisar = estado.puedeRevisar,
                        alEditar = alEditar,
                        alRevisar = alPedirRevisar,
                    )
                }
            }
        }
    }

    if (estado.confirmandoRevision) {
        DialogoRevisar(revisando = estado.revisando, alCancelar = alCancelarRevisar, alConfirmar = alConfirmarRevisar)
    }
}

@Composable
private fun DialogoRevisar(revisando: Boolean, alCancelar: () -> Unit, alConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.gasto_revisar_titulo)) },
        text = { Text(stringResource(Res.string.gasto_revisar_texto)) },
        confirmButton = {
            Button(onClick = alConfirmar, enabled = !revisando) {
                Text(stringResource(Res.string.gasto_revisar_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, enabled = !revisando) {
                Text(stringResource(Res.string.gasto_revisar_cancelar))
            }
        },
    )
}

/**
 * The text for a one-shot message. A 400 says what the backend found missing; a 409 explains the
 * estado conflict.
 */
@Composable
private fun MensajeGastoDetalle.texto(): String = when (this) {
    MensajeGastoDetalle.Revisado -> stringResource(Res.string.gasto_revisar_ok)
    is MensajeGastoDetalle.RevisionFallida -> when (error) {
        is ErrorAuth.Validacion -> {
            val faltan = error.campos.values.joinToString(". ")
            stringResource(Res.string.gasto_revisar_falta_datos) + if (faltan.isEmpty()) "" else " ($faltan)"
        }
        ErrorAuth.Conflicto, is ErrorAuth.ConflictoConDetalle -> stringResource(Res.string.gasto_revisar_conflicto)
        else -> stringResource(Res.string.gasto_revisar_fallido) + " " + error.mensaje()
    }
}

@Composable
private fun Contenido(
    gasto: Gasto,
    nombreCategoria: String?,
    puedeEditar: Boolean,
    puedeRevisar: Boolean,
    alEditar: () -> Unit,
    alRevisar: () -> Unit,
) {
    val sinDato = stringResource(Res.string.gastos_sin_dato)
    val si = stringResource(Res.string.gasto_detalle_si)
    val no = stringResource(Res.string.gasto_detalle_no)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = alEditar, enabled = puedeEditar) {
                Text(stringResource(Res.string.gasto_detalle_editar))
            }
            Button(onClick = alRevisar, enabled = puedeRevisar) {
                Text(stringResource(Res.string.gasto_detalle_revisar))
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo(stringResource(Res.string.gasto_detalle_id)) { Text(gasto.id.toString()) }
                Campo(stringResource(Res.string.gasto_detalle_estado)) { EtiquetaEstado(gasto.estado) }
                Campo(stringResource(Res.string.gasto_detalle_emisor)) {
                    Text(gasto.emisor?.takeIf { it.isNotBlank() } ?: sinDato)
                }
                Campo(stringResource(Res.string.gasto_detalle_fecha_emision)) {
                    Text(gasto.fechaEmision?.toString() ?: sinDato)
                }
                Campo(stringResource(Res.string.gasto_detalle_base_imponible)) {
                    Text(textoImporte(gasto.baseImponible, gasto.moneda))
                }
                Campo(stringResource(Res.string.gasto_detalle_iva)) { Text(textoImporte(gasto.iva, gasto.moneda)) }
                Campo(stringResource(Res.string.gasto_detalle_moneda)) { Text(gasto.moneda ?: sinDato) }
                Campo(stringResource(Res.string.gasto_detalle_categoria)) {
                    Text(nombreCategoria ?: gasto.categoriaId?.let { "#$it" } ?: sinDato)
                }
                Campo(stringResource(Res.string.gasto_detalle_cliente)) {
                    Text(gasto.clienteId?.let { "#$it" } ?: sinDato)
                }
                Campo(stringResource(Res.string.gasto_detalle_deducible)) {
                    Text(gasto.deducible?.let { if (it) si else no } ?: sinDato)
                }
                Campo(stringResource(Res.string.gasto_detalle_creado_en)) {
                    Text(gasto.creadoEn?.toString()?.replace('T', ' ') ?: sinDato)
                }
            }
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        valor()
    }
}
