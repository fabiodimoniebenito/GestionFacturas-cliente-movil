package com.fabio.gestionfacturas.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fabio.gestionfacturas.domain.model.ErrorAuth

private val ANCHO_MAXIMO_FORMULARIO = 420.dp

/**
 * Centered column with a sensible maximum width (so the form does not stretch across a wide
 * desktop window). Enter on a physical keyboard submits the form.
 */
@Composable
fun ContenedorFormulario(
    alEnviar: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ANCHO_MAXIMO_FORMULARIO)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .onPreviewKeyEvent { evento ->
                    val esEnter = evento.key == Key.Enter || evento.key == Key.NumPadEnter
                    if (esEnter && evento.type == KeyEventType.KeyDown) {
                        alEnviar()
                        true
                    } else {
                        false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido,
        )
    }
}

@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    error: String?,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    esPassword: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionIme: ImeAction = ImeAction.Next,
    alAccionIme: () -> Unit = {},
) {
    val soporte: (@Composable () -> Unit)? = if (error != null) {
        { Text(error) }
    } else {
        null
    }
    val transformacion = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        enabled = habilitado,
        isError = error != null,
        supportingText = soporte,
        visualTransformation = transformacion,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = accionIme),
        keyboardActions = KeyboardActions(onDone = { alAccionIme() }),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Primary button that shows a spinner and is disabled while [cargando]. */
@Composable
fun BotonPrincipal(
    texto: String,
    cargando: Boolean,
    alPulsar: () -> Unit,
) {
    Button(
        onClick = alPulsar,
        enabled = !cargando,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(texto)
        }
    }
}

/** General error line plus the optional backend detail. Nothing is shown when there is no error. */
@Composable
fun MensajeError(error: ErrorAuth?) {
    if (error == null) {
        return
    }
    Text(
        text = error.mensaje(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
    val detalle = error.detalleDelServidor()
    if (detalle != null) {
        Text(
            text = detalle,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
