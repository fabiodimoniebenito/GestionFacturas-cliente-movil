package com.fabio.gestionfacturas.ui.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.fabio.gestionfacturas.resources.cliente_form_cancelar
import com.fabio.gestionfacturas.resources.cliente_form_email
import com.fabio.gestionfacturas.resources.cliente_form_error_nif_duplicado
import com.fabio.gestionfacturas.resources.cliente_form_error_nif_vacio
import com.fabio.gestionfacturas.resources.cliente_form_error_nombre_vacio
import com.fabio.gestionfacturas.resources.cliente_form_guardar
import com.fabio.gestionfacturas.resources.cliente_form_nif
import com.fabio.gestionfacturas.resources.cliente_form_nif_no_editable
import com.fabio.gestionfacturas.resources.cliente_form_nombre
import com.fabio.gestionfacturas.resources.cliente_form_telefono
import com.fabio.gestionfacturas.resources.editar_cliente_titulo
import com.fabio.gestionfacturas.resources.nuevo_cliente_titulo
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

/** Create ([idCliente] null) or edit form. [alTerminar] is called once, after a save or when the cliente is gone. */
@Composable
fun ClienteFormScreen(
    idCliente: Long?,
    alVolver: () -> Unit,
    alTerminar: (ResultadoFormulario) -> Unit,
    viewModel: ClienteFormViewModel = koinViewModel(
        parameters = { if (idCliente != null) parametersOf(idCliente) else parametersOf() },
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

    ClienteFormContent(
        estado = estado,
        alVolver = alVolver,
        alNombreChange = viewModel::onNombreChange,
        alNifChange = viewModel::onNifChange,
        alEmailChange = viewModel::onEmailChange,
        alTelefonoChange = viewModel::onTelefonoChange,
        alGuardar = viewModel::onGuardar,
        alReintentarCarga = viewModel::onReintentarCarga,
    )
}

@Composable
fun ClienteFormContent(
    estado: ClienteFormUiState,
    alVolver: () -> Unit,
    alNombreChange: (String) -> Unit,
    alNifChange: (String) -> Unit,
    alEmailChange: (String) -> Unit,
    alTelefonoChange: (String) -> Unit,
    alGuardar: () -> Unit,
    alReintentarCarga: () -> Unit,
) {
    val titulo = if (estado.esEdicion) {
        stringResource(Res.string.editar_cliente_titulo)
    } else {
        stringResource(Res.string.nuevo_cliente_titulo)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BarraTitulo(
            titulo = titulo,
            alVolver = { if (!estado.guardando) alVolver() },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val carga = estado.carga) {
                CargaFormCliente.Cargando -> EstadoCargando()
                is CargaFormCliente.Error -> EstadoError(
                    mensaje = carga.error.mensaje(),
                    alReintentar = alReintentarCarga,
                )
                CargaFormCliente.Lista -> Formulario(
                    estado = estado,
                    alVolver = alVolver,
                    alNombreChange = alNombreChange,
                    alNifChange = alNifChange,
                    alEmailChange = alEmailChange,
                    alTelefonoChange = alTelefonoChange,
                    alGuardar = alGuardar,
                )
            }
        }
    }
}

@Composable
private fun Formulario(
    estado: ClienteFormUiState,
    alVolver: () -> Unit,
    alNombreChange: (String) -> Unit,
    alNifChange: (String) -> Unit,
    alEmailChange: (String) -> Unit,
    alTelefonoChange: (String) -> Unit,
    alGuardar: () -> Unit,
) {
    val habilitado = !estado.guardando

    ContenedorFormulario(alEnviar = alGuardar) {
        CampoTexto(
            valor = estado.nombre,
            alCambiar = alNombreChange,
            etiqueta = stringResource(Res.string.cliente_form_nombre),
            error = mensajeDeCampo(CampoCliente.NOMBRE, estado),
            habilitado = habilitado,
        )
        // The NIF cannot be changed once the cliente exists.
        CampoTexto(
            valor = estado.nif,
            alCambiar = alNifChange,
            etiqueta = stringResource(Res.string.cliente_form_nif),
            error = if (estado.esEdicion) {
                stringResource(Res.string.cliente_form_nif_no_editable)
            } else {
                mensajeDeCampo(CampoCliente.NIF, estado)
            },
            habilitado = habilitado && !estado.esEdicion,
        )
        CampoTexto(
            valor = estado.email,
            alCambiar = alEmailChange,
            etiqueta = stringResource(Res.string.cliente_form_email),
            error = mensajeDeCampo(CampoCliente.EMAIL, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Email,
        )
        CampoTexto(
            valor = estado.telefono,
            alCambiar = alTelefonoChange,
            etiqueta = stringResource(Res.string.cliente_form_telefono),
            error = mensajeDeCampo(CampoCliente.TELEFONO, estado),
            habilitado = habilitado,
            tipoTeclado = KeyboardType.Phone,
            accionIme = ImeAction.Done,
            alAccionIme = alGuardar,
        )
        ErrorGeneral(estado)
        BotonPrincipal(
            texto = stringResource(Res.string.cliente_form_guardar),
            cargando = estado.guardando,
            alPulsar = alGuardar,
        )
        OutlinedButton(
            onClick = alVolver,
            enabled = habilitado,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.cliente_form_cancelar))
        }
    }
}

/** General error line; a 409 on create gets its own explanation about the NIF. */
@Composable
private fun ErrorGeneral(estado: ClienteFormUiState) {
    if (estado.error == ErrorAuth.Conflicto && !estado.esEdicion) {
        Text(
            text = stringResource(Res.string.cliente_form_error_nif_duplicado),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        MensajeError(estado.error)
    }
}

/** Message under a field: the local check first, then what the backend said about that field. */
@Composable
private fun mensajeDeCampo(campo: CampoCliente, estado: ClienteFormUiState): String? {
    val local = estado.erroresLocales[campo]
    if (local != null) {
        return when (local) {
            ErrorCampoCliente.NOMBRE_VACIO -> stringResource(Res.string.cliente_form_error_nombre_vacio)
            ErrorCampoCliente.NIF_VACIO -> stringResource(Res.string.cliente_form_error_nif_vacio)
        }
    }
    val error = estado.error
    if (error is ErrorAuth.Validacion) {
        return error.campos[campo.claveApi]
    }
    return null
}
