package com.fabio.gestionfacturas.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.estado_reintentar
import org.jetbrains.compose.resources.stringResource

/** Centered spinner for a screen that is loading its first data. */
@Composable
fun EstadoCargando(modifier: Modifier = Modifier) {
    ColumnCentrada(modifier) {
        CircularProgressIndicator()
    }
}

/** Centered message for an empty list, with an optional action (e.g. "create the first one"). */
@Composable
fun EstadoVacio(
    mensaje: String,
    modifier: Modifier = Modifier,
    textoAccion: String? = null,
    alPulsarAccion: (() -> Unit)? = null,
) {
    ColumnCentrada(modifier) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (textoAccion != null && alPulsarAccion != null) {
            Button(onClick = alPulsarAccion) { Text(textoAccion) }
        }
    }
}

/** Centered error message with a retry button. */
@Composable
fun EstadoError(
    mensaje: String,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnCentrada(modifier) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(onClick = alReintentar) { Text(stringResource(Res.string.estado_reintentar)) }
    }
}

@Composable
private fun ColumnCentrada(
    modifier: Modifier,
    contenido: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        contenido()
    }
}
