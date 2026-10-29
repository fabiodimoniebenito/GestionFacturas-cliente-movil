package com.fabio.gestionfacturas.ui.gastos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** One choice of a [SelectorDesplegable]. */
data class OpcionSelector(val id: Long, val texto: String)

/**
 * Dropdown with an empty option ([textoNinguno]) that sets the selection to null. [seleccionado] that
 * is not among [opciones] (e.g. a deactivated cliente) is shown as "#id" so it is not silently lost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDesplegable(
    etiqueta: String,
    textoNinguno: String,
    opciones: List<OpcionSelector>,
    seleccionado: Long?,
    alSeleccionar: (Long?) -> Unit,
    habilitado: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }
    val textoActual = when (seleccionado) {
        null -> textoNinguno
        else -> opciones.firstOrNull { it.id == seleccionado }?.texto ?: "#$seleccionado"
    }
    ExposedDropdownMenuBox(
        expanded = expandido && habilitado,
        onExpandedChange = { expandido = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = textoActual,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            singleLine = true,
            label = { Text(etiqueta) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expandido && habilitado, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text(textoNinguno) },
                onClick = {
                    alSeleccionar(null)
                    expandido = false
                },
            )
            for (opcion in opciones) {
                DropdownMenuItem(
                    text = { Text(opcion.texto) },
                    onClick = {
                        alSeleccionar(opcion.id)
                        expandido = false
                    },
                )
            }
        }
    }
}
