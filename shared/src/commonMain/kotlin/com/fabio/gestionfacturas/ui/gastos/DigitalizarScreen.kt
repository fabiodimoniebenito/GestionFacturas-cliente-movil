package com.fabio.gestionfacturas.ui.gastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.digitalizar_archivo
import com.fabio.gestionfacturas.resources.digitalizar_cambiar
import com.fabio.gestionfacturas.resources.digitalizar_crear_manual
import com.fabio.gestionfacturas.resources.digitalizar_elegir
import com.fabio.gestionfacturas.resources.digitalizar_explicacion
import com.fabio.gestionfacturas.resources.digitalizar_reintentar
import com.fabio.gestionfacturas.resources.digitalizar_subir
import com.fabio.gestionfacturas.resources.digitalizar_subiendo
import com.fabio.gestionfacturas.resources.digitalizar_titulo
import com.fabio.gestionfacturas.ui.common.BarraTitulo
import com.fabio.gestionfacturas.ui.common.mensaje
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val AnchoMaximo = 560.dp

/**
 * Picks an image and uploads it. [alCrearGasto] is called once with the id of the gasto the backend
 * created; [alCrearManual] is the way out when the invoice could not be read.
 */
@Composable
fun DigitalizarScreen(
    alVolver: () -> Unit,
    alCrearGasto: (Long) -> Unit,
    alCrearManual: () -> Unit,
    viewModel: DigitalizarViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.gastoCreadoId) {
        val id = estado.gastoCreadoId
        if (id != null) {
            viewModel.onNavegacionConsumida()
            alCrearGasto(id)
        }
    }

    DigitalizarContent(
        estado = estado,
        alVolver = alVolver,
        alElegir = viewModel::onElegirImagen,
        alSubir = viewModel::onSubir,
        alCrearManual = alCrearManual,
    )
}

@Composable
fun DigitalizarContent(
    estado: DigitalizarUiState,
    alVolver: () -> Unit,
    alElegir: () -> Unit,
    alSubir: () -> Unit,
    alCrearManual: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // The back control is not offered while the upload is in flight.
        BarraTitulo(
            titulo = stringResource(Res.string.digitalizar_titulo),
            alVolver = if (estado.puedeVolver) alVolver else null,
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = AnchoMaximo).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Cuerpo(estado = estado, alElegir = alElegir, alSubir = alSubir, alCrearManual = alCrearManual)
            }
        }
    }
}

@Composable
private fun Cuerpo(
    estado: DigitalizarUiState,
    alElegir: () -> Unit,
    alSubir: () -> Unit,
    alCrearManual: () -> Unit,
) {
    if (estado.fase == FaseDigitalizar.INICIAL) {
        Text(stringResource(Res.string.digitalizar_explicacion), style = MaterialTheme.typography.bodyLarge)
    }
    val archivo = estado.archivo
    if (archivo != null) {
        Text(
            text = stringResource(
                Res.string.digitalizar_archivo,
                archivo.nombre,
                formatearTamano(archivo.bytes.size),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (estado.subiendo) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(stringResource(Res.string.digitalizar_subiendo), style = MaterialTheme.typography.bodyLarge)
    }
    val error = estado.error
    if (error != null && !estado.subiendo) {
        Text(
            text = error.mensaje(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            archivo == null -> Button(onClick = alElegir, enabled = !estado.subiendo) {
                Text(stringResource(Res.string.digitalizar_elegir))
            }
            else -> {
                val textoSubir = if (estado.fase == FaseDigitalizar.ERROR) {
                    Res.string.digitalizar_reintentar
                } else {
                    Res.string.digitalizar_subir
                }
                Button(onClick = alSubir, enabled = !estado.subiendo) { Text(stringResource(textoSubir)) }
                OutlinedButton(onClick = alElegir, enabled = !estado.subiendo) {
                    Text(stringResource(Res.string.digitalizar_cambiar))
                }
            }
        }
    }
    if (error == ErrorDigitalizar.NoSePudoLeer && !estado.subiendo) {
        OutlinedButton(onClick = alCrearManual) { Text(stringResource(Res.string.digitalizar_crear_manual)) }
    }
}
