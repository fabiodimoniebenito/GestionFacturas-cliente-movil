package com.fabio.gestionfacturas.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.comun_volver
import org.jetbrains.compose.resources.stringResource

/** Screen title with an explicit "Volver" button when [alVolver] is given (desktop has no back button). */
@Composable
fun BarraTitulo(
    titulo: String,
    modifier: Modifier = Modifier,
    alVolver: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (alVolver != null) {
            OutlinedButton(onClick = alVolver) { Text(stringResource(Res.string.comun_volver)) }
        }
        Text(text = titulo, style = MaterialTheme.typography.headlineMedium)
    }
}
