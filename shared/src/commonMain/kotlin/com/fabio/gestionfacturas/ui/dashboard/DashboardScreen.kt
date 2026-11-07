package com.fabio.gestionfacturas.ui.dashboard

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.ResumenPeriodo
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.dashboard_actualizar
import com.fabio.gestionfacturas.resources.dashboard_aplicar
import com.fabio.gestionfacturas.resources.dashboard_beneficio_caja
import com.fabio.gestionfacturas.resources.dashboard_beneficio_caja_ayuda
import com.fabio.gestionfacturas.resources.dashboard_beneficio_facturado
import com.fabio.gestionfacturas.resources.dashboard_beneficio_facturado_ayuda
import com.fabio.gestionfacturas.resources.dashboard_cobrado
import com.fabio.gestionfacturas.resources.dashboard_desde
import com.fabio.gestionfacturas.resources.dashboard_error_desde
import com.fabio.gestionfacturas.resources.dashboard_error_hasta
import com.fabio.gestionfacturas.resources.dashboard_error_rango
import com.fabio.gestionfacturas.resources.dashboard_facturado
import com.fabio.gestionfacturas.resources.dashboard_gastos
import com.fabio.gestionfacturas.resources.dashboard_hasta
import com.fabio.gestionfacturas.resources.dashboard_modo
import com.fabio.gestionfacturas.resources.dashboard_modo_con_iva
import com.fabio.gestionfacturas.resources.dashboard_modo_sin_iva
import com.fabio.gestionfacturas.resources.dashboard_pendientes_titulo
import com.fabio.gestionfacturas.resources.dashboard_pendientes_total
import com.fabio.gestionfacturas.resources.dashboard_pendientes_vacio
import com.fabio.gestionfacturas.resources.dashboard_periodo
import com.fabio.gestionfacturas.resources.dashboard_periodo_anio
import com.fabio.gestionfacturas.resources.dashboard_periodo_mes
import com.fabio.gestionfacturas.resources.dashboard_periodo_personalizado
import com.fabio.gestionfacturas.resources.dashboard_periodo_trimestre
import com.fabio.gestionfacturas.resources.dashboard_rango
import com.fabio.gestionfacturas.resources.dashboard_resumen_titulo
import com.fabio.gestionfacturas.resources.dashboard_titulo
import com.fabio.gestionfacturas.resources.estado_reintentar
import com.fabio.gestionfacturas.resources.ingresos_cliente_desconocido
import com.fabio.gestionfacturas.resources.ingresos_sin_cliente
import com.fabio.gestionfacturas.resources.ingresos_sin_concepto
import com.fabio.gestionfacturas.resources.ingresos_sin_dato
import com.fabio.gestionfacturas.ui.common.mensaje
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val AnchoMaximo = 1000.dp

@Composable
fun DashboardScreen(
    alAbrirIngreso: (Long) -> Unit,
    /** True once when the detail of an ingreso was left after a change: the figures are stale. */
    volvioDelDetalle: Boolean = false,
    alConsumirVueltaDelDetalle: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(volvioDelDetalle) {
        if (volvioDelDetalle) {
            viewModel.onVolverDelDetalle()
            alConsumirVueltaDelDetalle()
        }
    }
    DashboardContent(
        estado = estado,
        alAbrirIngreso = alAbrirIngreso,
        alRefrescar = viewModel::onRefrescar,
        alElegirPreset = viewModel::onPresetSeleccionado,
        alCambiarDesde = viewModel::onDesdeCambiado,
        alCambiarHasta = viewModel::onHastaCambiado,
        alAplicar = viewModel::onAplicarPersonalizado,
        alCambiarModo = viewModel::onModoCambiado,
        alReintentarResumen = viewModel::onReintentarResumen,
        alReintentarPendientes = viewModel::onReintentarPendientes,
    )
}

@Composable
fun DashboardContent(
    estado: DashboardUiState,
    alAbrirIngreso: (Long) -> Unit,
    alRefrescar: () -> Unit,
    alElegirPreset: (PresetPeriodo) -> Unit,
    alCambiarDesde: (String) -> Unit,
    alCambiarHasta: (String) -> Unit,
    alAplicar: () -> Unit,
    alCambiarModo: (ModoImporte) -> Unit,
    alReintentarResumen: () -> Unit,
    alReintentarPendientes: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = AnchoMaximo)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Cabecera(alRefrescar)
            SelectorPeriodo(estado, alElegirPreset, alCambiarDesde, alCambiarHasta, alAplicar)
            SelectorModo(estado.modo, alCambiarModo)
            SeccionResumen(estado, alReintentarResumen)
            SeccionPendientes(estado.pendientes, alAbrirIngreso, alReintentarPendientes)
        }
    }
}

@Composable
private fun Cabecera(alRefrescar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.dashboard_titulo),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = alRefrescar) { Text(stringResource(Res.string.dashboard_actualizar)) }
    }
}

// ---- controls --------------------------------------------------------------------------------

@Composable
private fun SelectorPeriodo(
    estado: DashboardUiState,
    alElegirPreset: (PresetPeriodo) -> Unit,
    alCambiarDesde: (String) -> Unit,
    alCambiarHasta: (String) -> Unit,
    alAplicar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.dashboard_periodo), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetPeriodo.entries.forEach { preset ->
                FilterChip(
                    selected = estado.preset == preset,
                    onClick = { alElegirPreset(preset) },
                    label = { Text(preset.texto()) },
                )
            }
        }
        if (estado.preset == PresetPeriodo.PERSONALIZADO) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = estado.desdeTexto,
                    onValueChange = alCambiarDesde,
                    label = { Text(stringResource(Res.string.dashboard_desde)) },
                    singleLine = true,
                    isError = estado.errorPersonalizado == ErrorPeriodo.DESDE_INVALIDA ||
                        estado.errorPersonalizado == ErrorPeriodo.RANGO_INVERTIDO,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = estado.hastaTexto,
                    onValueChange = alCambiarHasta,
                    label = { Text(stringResource(Res.string.dashboard_hasta)) },
                    singleLine = true,
                    isError = estado.errorPersonalizado == ErrorPeriodo.HASTA_INVALIDA ||
                        estado.errorPersonalizado == ErrorPeriodo.RANGO_INVERTIDO,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = alAplicar) { Text(stringResource(Res.string.dashboard_aplicar)) }
            }
            estado.errorPersonalizado?.let { error ->
                Text(
                    text = error.texto(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            text = stringResource(
                Res.string.dashboard_rango,
                estado.periodo.desde.toString(),
                estado.periodo.hasta.toString(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectorModo(modo: ModoImporte, alCambiar: (ModoImporte) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.dashboard_modo), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModoImporte.entries.forEach { opcion ->
                FilterChip(
                    selected = modo == opcion,
                    onClick = { alCambiar(opcion) },
                    label = { Text(opcion.texto()) },
                )
            }
        }
    }
}

@Composable
private fun PresetPeriodo.texto(): String = when (this) {
    PresetPeriodo.ESTE_MES -> stringResource(Res.string.dashboard_periodo_mes)
    PresetPeriodo.TRIMESTRE_ACTUAL -> stringResource(Res.string.dashboard_periodo_trimestre)
    PresetPeriodo.ESTE_ANIO -> stringResource(Res.string.dashboard_periodo_anio)
    PresetPeriodo.PERSONALIZADO -> stringResource(Res.string.dashboard_periodo_personalizado)
}

@Composable
private fun ModoImporte.texto(): String = when (this) {
    ModoImporte.CON_IVA -> stringResource(Res.string.dashboard_modo_con_iva)
    ModoImporte.SIN_IVA -> stringResource(Res.string.dashboard_modo_sin_iva)
}

@Composable
private fun ErrorPeriodo.texto(): String = when (this) {
    ErrorPeriodo.DESDE_INVALIDA -> stringResource(Res.string.dashboard_error_desde)
    ErrorPeriodo.HASTA_INVALIDA -> stringResource(Res.string.dashboard_error_hasta)
    ErrorPeriodo.RANGO_INVERTIDO -> stringResource(Res.string.dashboard_error_rango)
}

// ---- resumen ---------------------------------------------------------------------------------

@Composable
private fun SeccionResumen(estado: DashboardUiState, alReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.dashboard_resumen_titulo), style = MaterialTheme.typography.titleLarge)
        when (val carga = estado.resumen) {
            CargaResumen.Cargando -> SeccionCargando()
            is CargaResumen.Error -> SeccionError(carga.error.mensaje(), alReintentar)
            is CargaResumen.Contenido -> CifrasDelResumen(carga.resumen, estado.modo)
        }
    }
}

@Composable
private fun CifrasDelResumen(resumen: ResumenPeriodo, modo: ModoImporte) {
    val cifras = resumen.cifras(modo)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TarjetaCifra(stringResource(Res.string.dashboard_facturado), cifras.facturado, Modifier.weight(1f))
        TarjetaCifra(stringResource(Res.string.dashboard_cobrado), cifras.cobrado, Modifier.weight(1f))
        TarjetaCifra(stringResource(Res.string.dashboard_gastos), cifras.gastos, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TarjetaCifra(
            titulo = stringResource(Res.string.dashboard_beneficio_caja),
            importe = cifras.beneficioCaja,
            modifier = Modifier.weight(1f),
            ayuda = stringResource(Res.string.dashboard_beneficio_caja_ayuda),
        )
        TarjetaCifra(
            titulo = stringResource(Res.string.dashboard_beneficio_facturado),
            importe = cifras.beneficioFacturado,
            modifier = Modifier.weight(1f),
            ayuda = stringResource(Res.string.dashboard_beneficio_facturado_ayuda),
        )
    }
}

/** A negative figure uses the error color; [ayuda] is the short caption of the beneficio cards. */
@Composable
private fun TarjetaCifra(titulo: String, importe: Importe, modifier: Modifier = Modifier, ayuda: String? = null) {
    Card(modifier = modifier, colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(
                text = textoEuros(importe),
                style = MaterialTheme.typography.headlineSmall,
                color = if (importe.esNegativo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (ayuda != null) {
                Text(
                    text = ayuda,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---- pendientes ------------------------------------------------------------------------------

@Composable
private fun SeccionPendientes(carga: CargaPendientes, alAbrir: (Long) -> Unit, alReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.dashboard_pendientes_titulo), style = MaterialTheme.typography.titleLarge)
        when (carga) {
            CargaPendientes.Cargando -> SeccionCargando()
            CargaPendientes.Vacio -> Text(
                text = stringResource(Res.string.dashboard_pendientes_vacio),
                style = MaterialTheme.typography.bodyLarge,
            )
            is CargaPendientes.Error -> SeccionError(carga.error.mensaje(), alReintentar)
            is CargaPendientes.Contenido -> {
                Text(
                    text = stringResource(Res.string.dashboard_pendientes_total) + ": " + textoEuros(carga.total),
                    style = MaterialTheme.typography.titleMedium,
                )
                carga.filas.forEach { fila -> FilaPendienteCard(fila, alAbrir) }
            }
        }
    }
}

@Composable
private fun FilaPendienteCard(fila: FilaPendiente, alAbrir: (Long) -> Unit) {
    val ingreso = fila.ingreso
    val cliente = when {
        fila.nombreCliente != null -> fila.nombreCliente
        ingreso.clienteId == null -> stringResource(Res.string.ingresos_sin_cliente)
        else -> stringResource(Res.string.ingresos_cliente_desconocido)
    }
    Card(onClick = { alAbrir(ingreso.id) }, modifier = Modifier.fillMaxWidth()) {
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
            Text(
                text = ingreso.total?.let { textoEuros(it) } ?: stringResource(Res.string.ingresos_sin_dato),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

// ---- shared pieces ---------------------------------------------------------------------------

/** The display helper plus the euro sign. The backend sends no currency here, so it is always euros. */
private fun textoEuros(importe: Importe): String = importe.mostrar() + " €"

/** Compact loading state for a section inside the scrolling column (the common ones fill the screen). */
@Composable
private fun SeccionCargando() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SeccionError(mensaje: String, alReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = alReintentar) { Text(stringResource(Res.string.estado_reintentar)) }
    }
}
