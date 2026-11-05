package com.fabio.gestionfacturas.ui.ingresos

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
import androidx.compose.material3.OutlinedTextField
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
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.cobro_cancelar
import com.fabio.gestionfacturas.resources.cobro_conflicto
import com.fabio.gestionfacturas.resources.cobro_error_fecha_invalida
import com.fabio.gestionfacturas.resources.cobro_fallido
import com.fabio.gestionfacturas.resources.cobro_registrado_ok
import com.fabio.gestionfacturas.resources.cobro_registrar_confirmar
import com.fabio.gestionfacturas.resources.cobro_registrar_fecha
import com.fabio.gestionfacturas.resources.cobro_registrar_titulo
import com.fabio.gestionfacturas.resources.cobro_revertido_ok
import com.fabio.gestionfacturas.resources.cobro_revertir_confirmar
import com.fabio.gestionfacturas.resources.cobro_revertir_texto
import com.fabio.gestionfacturas.resources.cobro_revertir_titulo
import com.fabio.gestionfacturas.resources.ingreso_detalle_base_imponible
import com.fabio.gestionfacturas.resources.ingreso_detalle_cliente
import com.fabio.gestionfacturas.resources.ingreso_detalle_concepto
import com.fabio.gestionfacturas.resources.ingreso_detalle_creado_en
import com.fabio.gestionfacturas.resources.ingreso_detalle_estado
import com.fabio.gestionfacturas.resources.ingreso_detalle_fecha_cobro
import com.fabio.gestionfacturas.resources.ingreso_detalle_fecha_emision
import com.fabio.gestionfacturas.resources.ingreso_detalle_id
import com.fabio.gestionfacturas.resources.ingreso_detalle_iva
import com.fabio.gestionfacturas.resources.ingreso_detalle_moneda
import com.fabio.gestionfacturas.resources.ingreso_detalle_registrar_cobro
import com.fabio.gestionfacturas.resources.ingreso_detalle_revertir_cobro
import com.fabio.gestionfacturas.resources.ingreso_detalle_titulo
import com.fabio.gestionfacturas.resources.ingreso_detalle_total
import com.fabio.gestionfacturas.resources.ingreso_detalle_ya_no_existe
import com.fabio.gestionfacturas.resources.ingresos_cliente_desconocido
import com.fabio.gestionfacturas.resources.ingresos_sin_cliente
import com.fabio.gestionfacturas.resources.ingresos_sin_dato
import com.fabio.gestionfacturas.ui.common.BarraTitulo
import com.fabio.gestionfacturas.ui.common.EstadoCargando
import com.fabio.gestionfacturas.ui.common.EstadoError
import com.fabio.gestionfacturas.ui.common.EstadoVacio
import com.fabio.gestionfacturas.ui.common.mensaje
import com.fabio.gestionfacturas.ui.gastos.textoImporte
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val AnchoMaximo = 900.dp

@Composable
fun IngresoDetalleScreen(
    idIngreso: Long,
    alVolver: () -> Unit,
    alHuboCambios: () -> Unit,
    alYaNoExiste: () -> Unit,
    viewModel: IngresoDetalleViewModel = koinViewModel(parameters = { parametersOf(idIngreso) }),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    IngresoDetalleContent(
        estado = estado,
        alVolver = alVolver,
        alReintentar = viewModel::onReintentar,
        alPedirRegistrarCobro = viewModel::onPedirRegistrarCobro,
        alCambiarFecha = viewModel::onFechaCobroCambiada,
        alCancelarRegistrarCobro = viewModel::onCancelarRegistrarCobro,
        alConfirmarRegistrarCobro = viewModel::onConfirmarRegistrarCobro,
        alPedirRevertirCobro = viewModel::onPedirRevertirCobro,
        alCancelarRevertirCobro = viewModel::onCancelarRevertirCobro,
        alConfirmarRevertirCobro = viewModel::onConfirmarRevertirCobro,
        alConsumirMensaje = viewModel::onMensajeConsumido,
    )
    // A cobro operation succeeded: the list below must refresh.
    LaunchedEffect(estado.huboCambios) {
        if (estado.huboCambios) {
            viewModel.onCambiosNotificados()
            alHuboCambios()
        }
    }
    // 404 on load or on a cobro operation: back to the list, which explains it.
    LaunchedEffect(estado.carga) {
        if (estado.carga == CargaIngresoDetalle.NoEncontrado) {
            alYaNoExiste()
        }
    }
}

@Composable
fun IngresoDetalleContent(
    estado: IngresoDetalleUiState,
    alVolver: () -> Unit,
    alReintentar: () -> Unit,
    alPedirRegistrarCobro: () -> Unit,
    alCambiarFecha: (String) -> Unit,
    alCancelarRegistrarCobro: () -> Unit,
    alConfirmarRegistrarCobro: () -> Unit,
    alPedirRevertirCobro: () -> Unit,
    alCancelarRevertirCobro: () -> Unit,
    alConfirmarRevertirCobro: () -> Unit,
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
                BarraTitulo(titulo = stringResource(Res.string.ingreso_detalle_titulo), alVolver = alVolver)
                when (val carga = estado.carga) {
                    CargaIngresoDetalle.Cargando -> EstadoCargando()
                    CargaIngresoDetalle.NoEncontrado ->
                        EstadoVacio(mensaje = stringResource(Res.string.ingreso_detalle_ya_no_existe))
                    is CargaIngresoDetalle.Error ->
                        EstadoError(mensaje = carga.error.mensaje(), alReintentar = alReintentar)
                    is CargaIngresoDetalle.Contenido -> Contenido(
                        ingreso = carga.ingreso,
                        nombreCliente = carga.nombreCliente,
                        estado = estado,
                        alRegistrarCobro = alPedirRegistrarCobro,
                        alRevertirCobro = alPedirRevertirCobro,
                    )
                }
            }
        }
    }

    val dialogo = estado.dialogoCobro
    if (dialogo != null) {
        DialogoRegistrarCobro(
            dialogo = dialogo,
            operando = estado.operando,
            alCambiarFecha = alCambiarFecha,
            alCancelar = alCancelarRegistrarCobro,
            alConfirmar = alConfirmarRegistrarCobro,
        )
    }
    if (estado.confirmandoReversion) {
        DialogoRevertirCobro(
            operando = estado.operando,
            alCancelar = alCancelarRevertirCobro,
            alConfirmar = alConfirmarRevertirCobro,
        )
    }
}

@Composable
private fun DialogoRegistrarCobro(
    dialogo: DialogoRegistrarCobro,
    operando: Boolean,
    alCambiarFecha: (String) -> Unit,
    alCancelar: () -> Unit,
    alConfirmar: () -> Unit,
) {
    val errorServidor = dialogo.errorServidor
    val detalleServidor = (errorServidor as? ErrorAuth.Validacion)?.campos?.values?.joinToString(". ")
    val error = when {
        dialogo.fechaInvalida -> stringResource(Res.string.cobro_error_fecha_invalida)
        errorServidor != null -> detalleServidor?.ifEmpty { null } ?: errorServidor.mensaje()
        else -> null
    }
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.cobro_registrar_titulo)) },
        text = {
            OutlinedTextField(
                value = dialogo.fecha,
                onValueChange = alCambiarFecha,
                label = { Text(stringResource(Res.string.cobro_registrar_fecha)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = alConfirmar, enabled = !operando) {
                Text(stringResource(Res.string.cobro_registrar_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, enabled = !operando) { Text(stringResource(Res.string.cobro_cancelar)) }
        },
    )
}

@Composable
private fun DialogoRevertirCobro(operando: Boolean, alCancelar: () -> Unit, alConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.cobro_revertir_titulo)) },
        text = { Text(stringResource(Res.string.cobro_revertir_texto)) },
        confirmButton = {
            Button(onClick = alConfirmar, enabled = !operando) {
                Text(stringResource(Res.string.cobro_revertir_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, enabled = !operando) { Text(stringResource(Res.string.cobro_cancelar)) }
        },
    )
}

/** The text for a one-shot message; a failure adds the shared error text. */
@Composable
private fun MensajeIngresoDetalle.texto(): String = when (this) {
    MensajeIngresoDetalle.CobroRegistrado -> stringResource(Res.string.cobro_registrado_ok)
    MensajeIngresoDetalle.CobroRevertido -> stringResource(Res.string.cobro_revertido_ok)
    is MensajeIngresoDetalle.OperacionFallida -> stringResource(Res.string.cobro_fallido) + " " + error.mensaje()
}

@Composable
private fun Contenido(
    ingreso: Ingreso,
    nombreCliente: String?,
    estado: IngresoDetalleUiState,
    alRegistrarCobro: () -> Unit,
    alRevertirCobro: () -> Unit,
) {
    val sinDato = stringResource(Res.string.ingresos_sin_dato)
    val cliente = when {
        nombreCliente != null -> nombreCliente
        ingreso.clienteId == null -> stringResource(Res.string.ingresos_sin_cliente)
        else -> stringResource(Res.string.ingresos_cliente_desconocido)
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = alRegistrarCobro, enabled = estado.puedeRegistrarCobro) {
                Text(stringResource(Res.string.ingreso_detalle_registrar_cobro))
            }
            OutlinedButton(onClick = alRevertirCobro, enabled = estado.puedeRevertirCobro) {
                Text(stringResource(Res.string.ingreso_detalle_revertir_cobro))
            }
        }
        val conflicto = estado.conflicto
        if (conflicto != null) {
            // The backend's own message (already collected, date before emission...); generic if it sent none.
            Text(
                text = conflicto.detalle ?: stringResource(Res.string.cobro_conflicto),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo(stringResource(Res.string.ingreso_detalle_id)) { Text(ingreso.id.toString()) }
                Campo(stringResource(Res.string.ingreso_detalle_estado)) { EtiquetaEstadoCobro(ingreso.estadoCobro) }
                Campo(stringResource(Res.string.ingreso_detalle_concepto)) {
                    Text(ingreso.concepto?.takeIf { it.isNotBlank() } ?: sinDato)
                }
                Campo(stringResource(Res.string.ingreso_detalle_cliente)) { Text(cliente) }
                Campo(stringResource(Res.string.ingreso_detalle_fecha_emision)) {
                    Text(ingreso.fechaEmision?.toString() ?: sinDato)
                }
                Campo(stringResource(Res.string.ingreso_detalle_fecha_cobro)) {
                    Text(ingreso.fechaCobro?.toString() ?: sinDato)
                }
                Campo(stringResource(Res.string.ingreso_detalle_base_imponible)) {
                    Text(textoImporte(ingreso.baseImponible, ingreso.moneda))
                }
                Campo(stringResource(Res.string.ingreso_detalle_iva)) { Text(textoImporte(ingreso.iva, ingreso.moneda)) }
                Campo(stringResource(Res.string.ingreso_detalle_total)) {
                    Text(textoImporte(ingreso.total, ingreso.moneda))
                }
                Campo(stringResource(Res.string.ingreso_detalle_moneda)) { Text(ingreso.moneda ?: sinDato) }
                Campo(stringResource(Res.string.ingreso_detalle_creado_en)) {
                    Text(ingreso.creadoEn?.toString()?.replace('T', ' ') ?: sinDato)
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
