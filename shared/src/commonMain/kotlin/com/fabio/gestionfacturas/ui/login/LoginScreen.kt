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
import com.fabio.gestionfacturas.resources.campo_password
import com.fabio.gestionfacturas.resources.login_boton
import com.fabio.gestionfacturas.resources.login_ir_a_registro
import com.fabio.gestionfacturas.resources.login_titulo
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
fun LoginScreen(
    alIniciarSesion: () -> Unit,
    alIrARegistro: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.loginExitoso) {
        if (estado.loginExitoso) {
            viewModel.onNavegacionConsumida()
            alIniciarSesion()
        }
    }

    LoginContent(
        estado = estado,
        alCambiarEmail = viewModel::onEmailChange,
        alCambiarPassword = viewModel::onPasswordChange,
        alEnviar = viewModel::onIniciarSesion,
        alIrARegistro = alIrARegistro,
    )
}

/** Stateless rendering of [LoginUiState]. */
@Composable
fun LoginContent(
    estado: LoginUiState,
    alCambiarEmail: (String) -> Unit,
    alCambiarPassword: (String) -> Unit,
    alEnviar: () -> Unit,
    alIrARegistro: () -> Unit,
) {
    ContenedorFormulario(alEnviar = alEnviar) {
        Text(
            text = stringResource(Res.string.login_titulo),
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
            texto = stringResource(Res.string.login_boton),
            cargando = estado.cargando,
            alPulsar = alEnviar,
        )
        TextButton(
            onClick = alIrARegistro,
            enabled = !estado.cargando,
        ) {
            Text(stringResource(Res.string.login_ir_a_registro))
        }
    }
}
