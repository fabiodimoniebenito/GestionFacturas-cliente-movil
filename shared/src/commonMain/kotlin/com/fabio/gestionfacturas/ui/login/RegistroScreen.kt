package com.fabio.gestionfacturas.ui.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.campo_email
import com.fabio.gestionfacturas.resources.campo_nombre
import com.fabio.gestionfacturas.resources.campo_password
import com.fabio.gestionfacturas.resources.registro_boton
import com.fabio.gestionfacturas.resources.registro_titulo
import com.fabio.gestionfacturas.resources.registro_volver
import com.fabio.gestionfacturas.ui.common.BotonPrincipal
import com.fabio.gestionfacturas.ui.common.CampoFormulario
import com.fabio.gestionfacturas.ui.common.CampoTexto
import com.fabio.gestionfacturas.ui.common.ContenedorFormulario
import com.fabio.gestionfacturas.ui.common.MensajeError
import com.fabio.gestionfacturas.ui.common.mensajeDeCampo
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Stateful entry point: wires the ViewModel and turns its one-shot effect into navigation. */
@Composable
fun RegistroScreen(
    alRegistrarse: () -> Unit,
    alVolver: () -> Unit,
    viewModel: RegistroViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.registroExitoso) {
        if (estado.registroExitoso) {
            viewModel.onNavegacionConsumida()
            alRegistrarse()
        }
    }

    RegistroContent(
        estado = estado,
        alCambiarEmail = viewModel::onEmailChange,
        alCambiarNombre = viewModel::onNombreChange,
        alCambiarPassword = viewModel::onPasswordChange,
        alEnviar = viewModel::onRegistrar,
        alVolver = alVolver,
    )
}

/** Stateless rendering of [RegistroUiState]. */
@Composable
fun RegistroContent(
    estado: RegistroUiState,
    alCambiarEmail: (String) -> Unit,
    alCambiarNombre: (String) -> Unit,
    alCambiarPassword: (String) -> Unit,
    alEnviar: () -> Unit,
    alVolver: () -> Unit,
) {
    ContenedorFormulario(alEnviar = alEnviar) {
        // Desktop has no system back button, so going back is an explicit control.
        TextButton(
            onClick = alVolver,
            enabled = !estado.cargando,
        ) {
            Text(stringResource(Res.string.registro_volver))
        }
        Text(
            text = stringResource(Res.string.registro_titulo),
            style = MaterialTheme.typography.headlineMedium,
        )
        CampoTexto(
            valor = estado.email,
            alCambiar = alCambiarEmail,
            etiqueta = stringResource(Res.string.campo_email),
            error = mensajeDeCampo(CampoFormulario.EMAIL, estado.erroresCampo, estado.error),
            habilitado = !estado.cargando,
            tipoTeclado = KeyboardType.Email,
        )
        CampoTexto(
            valor = estado.nombre,
            alCambiar = alCambiarNombre,
            etiqueta = stringResource(Res.string.campo_nombre),
            error = mensajeDeCampo(CampoFormulario.NOMBRE, estado.erroresCampo, estado.error),
            habilitado = !estado.cargando,
        )
        CampoTexto(
            valor = estado.password,
            alCambiar = alCambiarPassword,
            etiqueta = stringResource(Res.string.campo_password),
            error = mensajeDeCampo(CampoFormulario.PASSWORD, estado.erroresCampo, estado.error),
            habilitado = !estado.cargando,
            esPassword = true,
            tipoTeclado = KeyboardType.Password,
            accionIme = ImeAction.Done,
            alAccionIme = alEnviar,
        )
        MensajeError(estado.error)
        BotonPrincipal(
            texto = stringResource(Res.string.registro_boton),
            cargando = estado.cargando,
            alPulsar = alEnviar,
        )
    }
}
