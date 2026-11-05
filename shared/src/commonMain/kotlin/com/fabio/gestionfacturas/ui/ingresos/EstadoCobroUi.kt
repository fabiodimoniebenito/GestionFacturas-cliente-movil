package com.fabio.gestionfacturas.ui.ingresos

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.ingreso_estado_cobrada
import com.fabio.gestionfacturas.resources.ingreso_estado_desconocido
import com.fabio.gestionfacturas.resources.ingreso_estado_pendiente
import com.fabio.gestionfacturas.resources.ingresos_sin_dato
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Shared by the list and the detail: the texts and colours of each estado de cobro. */
private fun EstadoCobro.textoRes(): StringResource = when (this) {
    EstadoCobro.PENDIENTE -> Res.string.ingreso_estado_pendiente
    EstadoCobro.COBRADA -> Res.string.ingreso_estado_cobrada
    EstadoCobro.DESCONOCIDO -> Res.string.ingreso_estado_desconocido
}

/** Container and content colours of the chip. PENDIENTE uses the tertiary pair so it stands out. */
@Composable
internal fun EstadoCobro.colores(): Pair<Color, Color> {
    val esquema = MaterialTheme.colorScheme
    return when (this) {
        EstadoCobro.PENDIENTE -> esquema.tertiaryContainer to esquema.onTertiaryContainer
        EstadoCobro.COBRADA -> esquema.primaryContainer to esquema.onPrimaryContainer
        EstadoCobro.DESCONOCIDO -> esquema.surfaceVariant to esquema.onSurfaceVariant
    }
}

/** A chip with the estado name; a missing estado renders the neutral placeholder. */
@Composable
internal fun EtiquetaEstadoCobro(estado: EstadoCobro?, modifier: Modifier = Modifier) {
    if (estado == null) {
        Text(text = stringResource(Res.string.ingresos_sin_dato), modifier = modifier)
        return
    }
    val (fondo, contenido) = estado.colores()
    Surface(color = fondo, contentColor = contenido, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            text = stringResource(estado.textoRes()),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
