package com.fabio.gestionfacturas.ui.gastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.gasto_form_base_imponible
import com.fabio.gestionfacturas.resources.gasto_form_cancelar
import com.fabio.gestionfacturas.resources.gasto_form_categoria
import com.fabio.gestionfacturas.resources.gasto_form_cliente
import com.fabio.gestionfacturas.resources.gasto_form_conflicto
import com.fabio.gestionfacturas.resources.gasto_form_deducible
import com.fabio.gestionfacturas.resources.gasto_form_emisor
import com.fabio.gestionfacturas.resources.gasto_form_error_fecha_invalida
import com.fabio.gestionfacturas.resources.gasto_form_error_importe_invalido
import com.fabio.gestionfacturas.resources.gasto_form_error_total_vacio
import com.fabio.gestionfacturas.resources.gasto_form_fecha_emision
import com.fabio.gestionfacturas.resources.gasto_form_guardar
import com.fabio.gestionfacturas.resources.gasto_form_iva
import com.fabio.gestionfacturas.resources.gasto_form_moneda
import com.fabio.gestionfacturas.resources.gasto_form_ninguno
import com.fabio.gestionfacturas.resources.gasto_form_recargar_listas
import com.fabio.gestionfacturas.resources.gasto_form_selectores_fallidos
import com.fabio.gestionfacturas.resources.gasto_form_titulo_editar
import com.fabio.gestionfacturas.resources.gasto_form_titulo_nuevo
import com.fabio.gestionfacturas.resources.gasto_form_total
import com.fabio.gestionfacturas.resources.gasto_form_total_edicion
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import com.fabio.gestionfacturas.ui.common.BarraTitulo
import com.fabio.gestionfacturas.ui.common.BotonPrincipal
import com.fabio.gestionfacturas.ui.common.CampoTexto
import com.fabio.gestionfacturas.ui.common.ContenedorFormulario
import com.fabio.gestionfacturas.ui.common.EstadoCargando
import com.fabio.gestionfacturas.ui.common.EstadoError
import com.fabio.gestionfacturas.ui.common.MensajeError
import com.fabio.gestionfacturas.ui.common.mensaje
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Create ([idGasto] null) or edit form. [alTerminar] is called once, after a save or when the gasto is gone. */
@Composable
fun GastoFormScreen(
    idGasto: Long?,
    alVolver: () -> Unit,
    alTerminar: (ResultadoFormulario) -> Unit,
    viewModel: GastoFormViewModel = koinViewModel(
        parameters = { if (idGasto != null) parametersOf(idGasto) else parametersOf() },
    ),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.salida) {
        val salida = estado.salida
        if (salida != null) {
            viewModel.onSalidaConsumida()
            alTerminar(salida)
        }
    }

    GastoFormContent(
        estado = estado,
        alVolver = alVolver,
        alEmisorChange = viewModel::onEmisorChange,
        alFechaChange = viewModel::onFechaEmisionChange,
        alBaseChange = viewModel::onBaseImponibleChange,
        alIvaChange = viewModel::onIvaChange,
        alTotalChange = viewModel::onTotalChange,
        alMonedaChange = viewModel::onMonedaChange,
        alDeducibleChange = viewModel::onDeducibleChange,
        alClienteChange = viewModel::onClienteChange,
        alCategoriaChange = viewModel::onCategoriaChange,
        alRecargarSelectores = viewModel::onRecargarSelectores,
        alGuardar = viewModel::onGuardar,
        alReintentarCarga = viewModel::onReintentarCarga,
    )
}

@Composable
fun GastoFormContent(
    estado: GastoFormUiState,
    alVolver: () -> Unit,
    alEmisorChange: (String) -> Unit,
    alFechaChange: (String) -> Unit,
    alBaseChange: (String) -> Unit,
    alIvaChange: (String) -> Unit,
    alTotalChange: (String) -> Unit,
    alMonedaChange: (String) -> Unit,
    alDeducibleChange: (Boolean) -> Unit,
    alClienteChange: (Long?) -> Unit,
    alCategoriaChange: (Long?) -> Unit,
    alRecargarSelectores: () -> Unit,
    alGuardar: () -> Unit,
    alReintentarCarga: () -> Unit,
) {
    val titulo = if (estado.esEdicion) {
        stringResource(Res.string.gasto_form_titulo_editar)
    } else {
        stringResource(Res.string.gasto_form_titulo_nuevo)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BarraTitulo(titulo = titulo, alVolver = { if (!estado.guardando) alVolver() })
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val carga = estado.carga) {
                CargaFormGasto.Cargando -> EstadoCargando()
                is CargaFormGasto.Error -> EstadoError(mensaje = carga.error.mensaje(), alReintentar = alReintentarCarga)
                CargaFormGasto.Lista -> Formulario(
                    estado = estado,
                    alVolver = alVolver,
                    alEmisorChange = alEmisorChange,
                    alFechaChange = alFechaChange,
                    alBaseChange = alBaseChange,
                    alIvaChange = alIvaChange,
                    alTotalChange = alTotalChange,
                    alMonedaChange = alMonedaChange,
                    alDeducibleChange = alDeducibleChange,
                    alClienteChange = alClienteChange,
                    alCategoriaChange = alCategoriaChange,
                    alRecargarSelectores = alRecargarSelectores,
                    alGuardar = alGuardar,
                )
            }
        }
    }
}

@Composable
private fun Formulario(
    estado: GastoFormUiState,
    alVolver: () -> Unit,
    alEmisorChange: (String) -> Unit,
    alFechaChange: (String) -> Unit,
    alBaseChange: (String) -> Unit,
    alIvaChange: (String) -> Unit,
    alTotalChange: (String) -> Unit,
    alMonedaChange: (String) -> Unit,
    alDeducibleChange: (Boolean) -> Unit,
    alClienteChange: (Long?) -> Unit,
    alCategoriaChange: (Long?) -> Unit,
    alRecargarSelectores: () -> Unit,
    alGuardar: () -> Unit,
) {
    val habilitado = !estado.guardando
    val ninguno = stringResource(Res.string.gasto_form_ninguno)

    ContenedorFormulario(alEnviar = alGuardar) {
        CampoTexto(
            valor = estado.emisor,
            alCambiar = alEmisorChange,
            etiqueta = stringResource(Res.string.gasto_form_emisor),
            error = mensajeDeCampo(CampoGasto.EMISOR, estado),
            habilitado = habilitado,
        )
        CampoTexto(
            valor = estado.fechaEmision,
            alCambiar = alFechaChange,
            etiqueta = stringResource(Res.string.gasto_form_fecha_emision),
            error = mensajeDeCampo(CampoGasto.FECHA_EMISION, estado),
            habilitado = habilitado,
        )
        CampoTexto(
            valor = estado.baseImponible,
            alCambiar = alBaseChange,
            etiqueta = stringResource(Res.string.gasto_form_base_imponible),
            error = mensajeDeCampo(CampoGasto.BASE_IMPONIBLE, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Decimal,
        )
        CampoTexto(
            valor = estado.iva,
            alCambiar = alIvaChange,
            etiqueta = stringResource(Res.string.gasto_form_iva),
            error = mensajeDeCampo(CampoGasto.IVA, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Decimal,
        )
        CampoTexto(
            valor = estado.total,
            alCambiar = alTotalChange,
            etiqueta = stringResource(
                if (estado.esEdicion) Res.string.gasto_form_total_edicion else Res.string.gasto_form_total,
            ),
            error = mensajeDeCampo(CampoGasto.TOTAL, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Decimal,
        )
        CampoTexto(
            valor = estado.moneda,
            alCambiar = alMonedaChange,
            etiqueta = stringResource(Res.string.gasto_form_moneda),
            error = mensajeDeCampo(CampoGasto.MONEDA, estado),
            habilitado = habilitado,
            accionIme = ImeAction.Done,
            alAccionIme = alGuardar,
        )
        SelectorDesplegable(
            etiqueta = stringResource(Res.string.gasto_form_cliente),
            textoNinguno = ninguno,
            opciones = estado.clientes.map { OpcionSelector(it.id, it.nombre) },
            seleccionado = estado.clienteId,
            alSeleccionar = alClienteChange,
            habilitado = habilitado,
            error = mensajeDeCampo(CampoGasto.CLIENTE, estado),
        )
        // The create request has no category or deducible: they can only be set by editing.
        if (estado.esEdicion) {
            SelectorDesplegable(
                etiqueta = stringResource(Res.string.gasto_form_categoria),
                textoNinguno = ninguno,
                opciones = estado.categorias.map { OpcionSelector(it.id, it.nombre) },
                seleccionado = estado.categoriaId,
                alSeleccionar = alCategoriaChange,
                habilitado = habilitado,
                error = mensajeDeCampo(CampoGasto.CATEGORIA, estado),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.gasto_form_deducible))
                Switch(checked = estado.deducible, onCheckedChange = alDeducibleChange, enabled = habilitado)
            }
        }
        if (estado.selectoresFallidos) {
            Text(
                text = stringResource(Res.string.gasto_form_selectores_fallidos),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = alRecargarSelectores,
                enabled = habilitado && !estado.recargandoSelectores,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.gasto_form_recargar_listas))
            }
        }
        ErrorGeneral(estado)
        BotonPrincipal(
            texto = stringResource(Res.string.gasto_form_guardar),
            cargando = estado.guardando,
            alPulsar = alGuardar,
        )
        OutlinedButton(onClick = alVolver, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.gasto_form_cancelar))
        }
    }
}

/**
 * General error line: a 409 is explained as "can no longer be edited" (plus the backend's reason);
 * a plain `error` message of a 400, or the backend's message of a 404, goes below the general text.
 */
@Composable
private fun ErrorGeneral(estado: GastoFormUiState) {
    when (val error = estado.error) {
        is ErrorAuth.ConflictoConDetalle, ErrorAuth.Conflicto -> {
            Text(
                text = stringResource(Res.string.gasto_form_conflicto),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            val detalle = (error as? ErrorAuth.ConflictoConDetalle)?.detalle
            if (detalle != null) {
                Text(text = detalle, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        is ErrorAuth.NoEncontradoConDetalle -> {
            MensajeError(error)
            if (error.detalle != null) {
                Text(
                    text = error.detalle,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        else -> MensajeError(error)
    }
}

/** Message under a field: the local check first, then what the backend said about that field. */
@Composable
private fun mensajeDeCampo(campo: CampoGasto, estado: GastoFormUiState): String? {
    val local = estado.erroresLocales[campo]
    if (local != null) {
        return when (local) {
            ErrorCampoGasto.TOTAL_VACIO -> stringResource(Res.string.gasto_form_error_total_vacio)
            ErrorCampoGasto.IMPORTE_INVALIDO -> stringResource(Res.string.gasto_form_error_importe_invalido)
            ErrorCampoGasto.FECHA_INVALIDA -> stringResource(Res.string.gasto_form_error_fecha_invalida)
        }
    }
    val error = estado.error
    if (error is ErrorAuth.Validacion) {
        return error.campos[campo.claveApi]
    }
    return null
}
