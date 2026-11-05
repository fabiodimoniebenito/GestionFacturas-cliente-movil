package com.fabio.gestionfacturas.ui.ingresos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.ingreso_form_base_imponible
import com.fabio.gestionfacturas.resources.ingreso_form_cancelar
import com.fabio.gestionfacturas.resources.ingreso_form_cargando_clientes
import com.fabio.gestionfacturas.resources.ingreso_form_cliente
import com.fabio.gestionfacturas.resources.ingreso_form_clientes_fallidos
import com.fabio.gestionfacturas.resources.ingreso_form_concepto
import com.fabio.gestionfacturas.resources.ingreso_form_crear_cliente
import com.fabio.gestionfacturas.resources.ingreso_form_elegir_cliente
import com.fabio.gestionfacturas.resources.ingreso_form_error_cliente_vacio
import com.fabio.gestionfacturas.resources.ingreso_form_error_fecha_invalida
import com.fabio.gestionfacturas.resources.ingreso_form_error_importe_invalido
import com.fabio.gestionfacturas.resources.ingreso_form_error_total_vacio
import com.fabio.gestionfacturas.resources.ingreso_form_fecha_emision
import com.fabio.gestionfacturas.resources.ingreso_form_guardar
import com.fabio.gestionfacturas.resources.ingreso_form_iva
import com.fabio.gestionfacturas.resources.ingreso_form_moneda
import com.fabio.gestionfacturas.resources.ingreso_form_recargar_clientes
import com.fabio.gestionfacturas.resources.ingreso_form_sin_clientes
import com.fabio.gestionfacturas.resources.ingreso_form_titulo
import com.fabio.gestionfacturas.resources.ingreso_form_total
import com.fabio.gestionfacturas.ui.common.BarraTitulo
import com.fabio.gestionfacturas.ui.common.BotonPrincipal
import com.fabio.gestionfacturas.ui.common.CampoTexto
import com.fabio.gestionfacturas.ui.common.ContenedorFormulario
import com.fabio.gestionfacturas.ui.common.MensajeError
import com.fabio.gestionfacturas.ui.gastos.OpcionSelector
import com.fabio.gestionfacturas.ui.gastos.SelectorDesplegable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Create form. [alCrear] is called once with the id of the new ingreso. [clienteCreado] is true when
 * the user comes back from creating a cliente, so the selector reloads ([alConsumirClienteCreado] clears it).
 */
@Composable
fun IngresoFormScreen(
    alVolver: () -> Unit,
    alCrearCliente: () -> Unit,
    alCrear: (Long) -> Unit,
    clienteCreado: Boolean = false,
    alConsumirClienteCreado: () -> Unit = {},
    viewModel: IngresoFormViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.creadoId) {
        val id = estado.creadoId
        if (id != null) {
            viewModel.onCreadoConsumido()
            alCrear(id)
        }
    }
    LaunchedEffect(clienteCreado) {
        if (clienteCreado) {
            viewModel.onRecargarClientes()
            alConsumirClienteCreado()
        }
    }

    IngresoFormContent(
        estado = estado,
        alVolver = alVolver,
        alCrearCliente = alCrearCliente,
        alConceptoChange = viewModel::onConceptoChange,
        alFechaChange = viewModel::onFechaEmisionChange,
        alBaseChange = viewModel::onBaseImponibleChange,
        alIvaChange = viewModel::onIvaChange,
        alTotalChange = viewModel::onTotalChange,
        alMonedaChange = viewModel::onMonedaChange,
        alClienteChange = viewModel::onClienteChange,
        alRecargarClientes = viewModel::onRecargarClientes,
        alGuardar = viewModel::onGuardar,
    )
}

@Composable
fun IngresoFormContent(
    estado: IngresoFormUiState,
    alVolver: () -> Unit,
    alCrearCliente: () -> Unit,
    alConceptoChange: (String) -> Unit,
    alFechaChange: (String) -> Unit,
    alBaseChange: (String) -> Unit,
    alIvaChange: (String) -> Unit,
    alTotalChange: (String) -> Unit,
    alMonedaChange: (String) -> Unit,
    alClienteChange: (Long?) -> Unit,
    alRecargarClientes: () -> Unit,
    alGuardar: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BarraTitulo(
            titulo = stringResource(Res.string.ingreso_form_titulo),
            alVolver = { if (!estado.guardando) alVolver() },
        )
        if (estado.sinClientesActivos) {
            SinClientes(alCrearCliente = alCrearCliente)
        } else {
            Formulario(
                estado = estado,
                alVolver = alVolver,
                alConceptoChange = alConceptoChange,
                alFechaChange = alFechaChange,
                alBaseChange = alBaseChange,
                alIvaChange = alIvaChange,
                alTotalChange = alTotalChange,
                alMonedaChange = alMonedaChange,
                alClienteChange = alClienteChange,
                alRecargarClientes = alRecargarClientes,
                alGuardar = alGuardar,
            )
        }
    }
}

/** No active cliente: explain it and offer the shortcut to create one. */
@Composable
private fun SinClientes(alCrearCliente: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.ingreso_form_sin_clientes),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = alCrearCliente) { Text(stringResource(Res.string.ingreso_form_crear_cliente)) }
    }
}

@Composable
private fun Formulario(
    estado: IngresoFormUiState,
    alVolver: () -> Unit,
    alConceptoChange: (String) -> Unit,
    alFechaChange: (String) -> Unit,
    alBaseChange: (String) -> Unit,
    alIvaChange: (String) -> Unit,
    alTotalChange: (String) -> Unit,
    alMonedaChange: (String) -> Unit,
    alClienteChange: (Long?) -> Unit,
    alRecargarClientes: () -> Unit,
    alGuardar: () -> Unit,
) {
    val habilitado = !estado.guardando

    ContenedorFormulario(alEnviar = alGuardar) {
        SelectorDesplegable(
            etiqueta = stringResource(Res.string.ingreso_form_cliente),
            textoNinguno = stringResource(Res.string.ingreso_form_elegir_cliente),
            opciones = estado.clientes.map { OpcionSelector(it.id, it.nombre) },
            seleccionado = estado.clienteId,
            alSeleccionar = alClienteChange,
            habilitado = habilitado,
            error = mensajeDeCampo(CampoIngreso.CLIENTE, estado),
        )
        if (estado.cargandoClientes) {
            Text(
                text = stringResource(Res.string.ingreso_form_cargando_clientes),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (estado.clientesFallidos) {
            Text(
                text = stringResource(Res.string.ingreso_form_clientes_fallidos),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = alRecargarClientes,
                enabled = habilitado && !estado.cargandoClientes,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.ingreso_form_recargar_clientes))
            }
        }
        CampoTexto(
            valor = estado.concepto,
            alCambiar = alConceptoChange,
            etiqueta = stringResource(Res.string.ingreso_form_concepto),
            error = mensajeDeCampo(CampoIngreso.CONCEPTO, estado),
            habilitado = habilitado,
        )
        CampoTexto(
            valor = estado.fechaEmision,
            alCambiar = alFechaChange,
            etiqueta = stringResource(Res.string.ingreso_form_fecha_emision),
            error = mensajeDeCampo(CampoIngreso.FECHA_EMISION, estado),
            habilitado = habilitado,
        )
        CampoTexto(
            valor = estado.baseImponible,
            alCambiar = alBaseChange,
            etiqueta = stringResource(Res.string.ingreso_form_base_imponible),
            error = mensajeDeCampo(CampoIngreso.BASE_IMPONIBLE, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Decimal,
        )
        CampoTexto(
            valor = estado.iva,
            alCambiar = alIvaChange,
            etiqueta = stringResource(Res.string.ingreso_form_iva),
            error = mensajeDeCampo(CampoIngreso.IVA, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Decimal,
        )
        CampoTexto(
            valor = estado.total,
            alCambiar = alTotalChange,
            etiqueta = stringResource(Res.string.ingreso_form_total),
            error = mensajeDeCampo(CampoIngreso.TOTAL, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Decimal,
        )
        CampoTexto(
            valor = estado.moneda,
            alCambiar = alMonedaChange,
            etiqueta = stringResource(Res.string.ingreso_form_moneda),
            error = mensajeDeCampo(CampoIngreso.MONEDA, estado),
            habilitado = habilitado,
            accionIme = ImeAction.Done,
            alAccionIme = alGuardar,
        )
        ErrorGeneral(estado.error)
        BotonPrincipal(
            texto = stringResource(Res.string.ingreso_form_guardar),
            cargando = estado.guardando,
            alPulsar = alGuardar,
        )
        OutlinedButton(onClick = alVolver, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.ingreso_form_cancelar))
        }
    }
}

/** General error line; a 404 or 409 also shows the backend's own message (e.g. which cliente is gone). */
@Composable
private fun ErrorGeneral(error: ErrorAuth?) {
    MensajeError(error)
    val detalle = when (error) {
        is ErrorAuth.NoEncontradoConDetalle -> error.detalle
        is ErrorAuth.ConflictoConDetalle -> error.detalle
        else -> null
    }
    if (detalle != null) {
        Text(text = detalle, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/** Message under a field: the local check first, then what the backend said about that field. */
@Composable
private fun mensajeDeCampo(campo: CampoIngreso, estado: IngresoFormUiState): String? {
    val local = estado.erroresLocales[campo]
    if (local != null) {
        return when (local) {
            ErrorCampoIngreso.TOTAL_VACIO -> stringResource(Res.string.ingreso_form_error_total_vacio)
            ErrorCampoIngreso.IMPORTE_INVALIDO -> stringResource(Res.string.ingreso_form_error_importe_invalido)
            ErrorCampoIngreso.FECHA_INVALIDA -> stringResource(Res.string.ingreso_form_error_fecha_invalida)
            ErrorCampoIngreso.CLIENTE_VACIO -> stringResource(Res.string.ingreso_form_error_cliente_vacio)
        }
    }
    val error = estado.error
    if (error is ErrorAuth.Validacion) {
        return error.campos[campo.claveApi]
    }
    return null
}
